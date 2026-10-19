# ============================================================================
# admintem-backend API 全功能回归测试脚本
# ----------------------------------------------------------------------------
# 覆盖范围：登录/注册/登出、用户管理、角色管理、权限管理、按钮级权限校验、
#           Redis 令牌吊销与权限缓存失效、登录失败锁定等。
# 运行方式：pwsh api-regression-test.ps1 [-BaseUrl http://localhost:8090]
# 说明：测试产生的临时数据会在结尾统一清理（仅删除前缀为 t_ 的测试数据）。
# ============================================================================
param(
    [string]$BaseUrl = 'http://localhost:8090'
)

$script:Pass = 0
$script:Fail = 0
$script:Failures = @()

function Write-Check {
    param([string]$Name, [bool]$Ok, [string]$Detail = '')
    if ($Ok) {
        $script:Pass++
        Write-Host "  [PASS] $Name" -ForegroundColor Green
    }
    else {
        $script:Fail++
        $script:Failures += "$Name :: $Detail"
        Write-Host "  [FAIL] $Name :: $Detail" -ForegroundColor Red
    }
}

# 统一请求封装：返回 { Http, Code, Message, Data }
# 说明：使用 Windows 自带 curl.exe，避免 PowerShell 5.1 非交互模式下
#       Invoke-WebRequest 的凭据/交互限制。
function Invoke-Api {
    param(
        [string]$Method = 'POST',
        [string]$Path,
        [string]$Token = '',
        $Body = $null
    )
    $outFile = Join-Path $env:TEMP ("iwe_{0}.out" -f ([guid]::NewGuid().ToString('N')))
    $curlArgs = @('-sS', '-o', $outFile, '-w', '%{http_code}', '-X', $Method, "$BaseUrl$Path")
    if ($Token) {
        $curlArgs += @('-H', "admintem_token: LAUNCH $Token")
    }
    if ($null -ne $Body) {
        $jsonFile = Join-Path $env:TEMP ("iwe_{0}.json" -f ([guid]::NewGuid().ToString('N')))
        [System.IO.File]::WriteAllText($jsonFile, ($Body | ConvertTo-Json -Depth 10), (New-Object System.Text.UTF8Encoding($false)))
        $curlArgs += @('-H', 'Content-Type: application/json; charset=utf-8', '--data-binary', "@$jsonFile")
    }
    $httpStatus = -1
    try {
        $statusLine = (& curl.exe @curlArgs 2>&1 | Out-String).Trim()
        $httpStatus = [int]$statusLine
    }
    catch {
        Write-Host "    [curl 异常] $Path :: $($_.Exception.Message)" -ForegroundColor Yellow
    }
    $content = ''
    if (Test-Path $outFile) {
        $content = [System.IO.File]::ReadAllText($outFile, [System.Text.Encoding]::UTF8)
        Remove-Item $outFile -Force -ErrorAction SilentlyContinue
    }
    $obj = $null
    try { if ($content) { $obj = $content | ConvertFrom-Json } } catch { }
    return [pscustomobject]@{
        Http    = $httpStatus
        Code    = if ($obj) { [int]$obj.code } else { -1 }
        Message = if ($obj) { [string]$obj.message } else { $content }
        Data    = if ($obj) { $obj.data } else { $null }
    }
}

$ts = Get-Date -Format 'MMddHHmmss'
$adminUser = 'admin'
$adminPwd  = '12345678'
$newUser   = "t_user_$ts"
$newPwd    = 'Passw0rd!'
$lockUser  = "t_lock_$ts"
$regUser   = "t_reg_$ts"

Write-Host "=== admintem-backend 全功能回归测试 BaseUrl=$BaseUrl time=$ts ===" -ForegroundColor Cyan

# ---------------- 01 登录（管理员） ----------------
Write-Host "--- 01 管理员登录与基础信息 ---" -ForegroundColor Yellow
$r = Invoke-Api -Path '/auth/login' -Body @{ username = $adminUser; password = $adminPwd; rememberMe = $false }
Write-Check 'admin 登录成功' ($r.Http -eq 200 -and $r.Code -eq 200 -and $null -ne $r.Data.token) "http=$($r.Http) code=$($r.Code)"
$adminToken = $r.Data.token

$r = Invoke-Api -Path '/auth/getUserInfo' -Token $adminToken
Write-Check 'admin getUserInfo' ($r.Code -eq 200 -and $r.Data.userName -eq $adminUser) "code=$($r.Code)"
Write-Check 'admin 拥有超级角色菜单完整' ($r.Data.menus.Count -ge 2) "menus=$($r.Data.menus.Count)"
Write-Check 'admin 拥有按钮级权限标识' ($r.Data.permissions -contains 'sysAuthUser:addSysAuthUser') "perms=$($r.Data.permissions.Count)"

# ---------------- 02 越权/未登录 ----------------
Write-Host "--- 02 安全基线 ---" -ForegroundColor Yellow
$r = Invoke-Api -Path '/sysAuthUser/paginQuery' -Body @{ pageNum = 1; pageSize = 5 }
Write-Check '未携带令牌访问受保护接口返回 401' ($r.Http -eq 401) "http=$($r.Http)"

$r = Invoke-Api -Path '/auth/login' -Body @{ username = $adminUser; password = 'wrong-password' }
Write-Check '错误密码返回业务错误' ($r.Http -eq 200 -and $r.Code -ne 200) "code=$($r.Code)"

# ---------------- 03 用户管理（管理员操作） ----------------
Write-Host "--- 03 用户管理 ---" -ForegroundColor Yellow
$r = Invoke-Api -Path '/sysAuthUser/paginQuery' -Token $adminToken -Body @{ pageNum = 1; pageSize = 10 }
Write-Check 'admin 分页查询用户' ($r.Code -eq 200 -and $null -ne $r.Data.total) "code=$($r.Code) total=$($r.Data.total)"

$r = Invoke-Api -Path '/sysAuthUser/addSysAuthUser' -Token $adminToken -Body @{
    userName = $newUser; nickName = '测试用户'; password = $newPwd
    email = "$newUser@test.com"; phone = '13800138000'; sex = 1; status = 0
}
Write-Check 'admin 新增用户' ($r.Code -eq 200 -and $null -ne $r.Data) "code=$($r.Code)"
$newUserId = [long]$r.Data

$r = Invoke-Api -Path '/sysAuthUser/paginQuery' -Token $adminToken -Body @{ pageNum = 1; pageSize = 10; userName = $newUser }
Write-Check '分页条件查询到新用户' ($r.Code -eq 200 -and $r.Data.total -ge 1) "code=$($r.Code) total=$($r.Data.total)"

$r = Invoke-Api -Path '/sysAuthUser/editSysAuthUser' -Token $adminToken -Body @{
    id = $newUserId; userName = $newUser; nickName = '测试用户-改'; email = "$newUser@test.com"; phone = '13800138000'; sex = 1; status = 0
}
Write-Check 'admin 编辑用户' ($r.Code -eq 200) "code=$($r.Code)"

# 查询角色选项
$r = Invoke-Api -Path '/sysAuthRole/queryAll' -Method GET -Token $adminToken
$roleCommon = @($r.Data | Where-Object { $_.roleKey -eq 'common' })[0]
$roleAdmin  = @($r.Data | Where-Object { $_.roleKey -eq 'admin' })[0]
Write-Check '角色下拉含 common/admin' ($null -ne $roleCommon -and $null -ne $roleAdmin) ''

# 分配角色
$r = Invoke-Api -Path '/sysAuthUser/assignRole' -Token $adminToken -Body @{ userId = $newUserId; roleIds = @($roleCommon.id) }
Write-Check 'admin 分配 common 角色' ($r.Code -eq 200) "code=$($r.Code)"

$r = Invoke-Api -Path '/sysAuthUserRole/getUserRoleByUserId' -Token $adminToken -Body @{ userId = $newUserId }
Write-Check '查询用户已分配角色回显' ($r.Code -eq 200 -and @($r.Data.roleIds).Count -eq 1) "code=$($r.Code)"

# ---------------- 04 新用户（common）登录与权限校验 ----------------
Write-Host "--- 04 普通用户权限边界 ---" -ForegroundColor Yellow
$r = Invoke-Api -Path '/auth/login' -Body @{ username = $newUser; password = $newPwd }
Write-Check '普通用户登录成功' ($r.Code -eq 200 -and $null -ne $r.Data.token) "code=$($r.Code)"
$commonToken = $r.Data.token
Write-Check '普通用户菜单仅工作台' ($r.Data.user.menus.Count -eq 1 -and $r.Data.user.menus[0].permissionKey -eq 'home:homeview') "menus=$($r.Data.user.menus.Count)"
Write-Check '普通用户无系统管理权限' (-not ($r.Data.user.permissions -contains 'userInfo:userInfo')) ''

$r = Invoke-Api -Path '/sysAuthUser/paginQuery' -Token $commonToken -Body @{ pageNum = 1; pageSize = 5 }
Write-Check '普通用户访问用户列表被 403' ($r.Http -eq 403) "http=$($r.Http)"

# ---------------- 05 测试角色（test/cehsi1）按钮级权限 ----------------
Write-Host "--- 05 按钮级权限校验（test 账号：有菜单无写按钮） ---" -ForegroundColor Yellow
$r = Invoke-Api -Path '/auth/login' -Body @{ username = 'test'; password = '12345678' }
Write-Check 'test 登录成功' ($r.Code -eq 200) "code=$($r.Code)"
$testToken = $r.Data.token
Write-Check 'test 无新增用户按钮权限' (-not ($r.Data.user.permissions -contains 'sysAuthUser:addSysAuthUser')) ''
Write-Check 'test 有用户列表查询权限' ($r.Data.user.permissions -contains 'sysAuthUser:paginQuery') ''

$r = Invoke-Api -Path '/sysAuthUser/paginQuery' -Token $testToken -Body @{ pageNum = 1; pageSize = 5 }
Write-Check 'test 可查询用户列表（菜单权限）' ($r.Code -eq 200) "code=$($r.Code)"

$r = Invoke-Api -Path '/sysAuthUser/addSysAuthUser' -Token $testToken -Body @{
    userName = 't_noperm_1'; nickName = 'x'; password = '123456'; status = 0
}
Write-Check 'test 新增用户被 403（按钮权限）' ($r.Http -eq 403) "http=$($r.Http)"

$r = Invoke-Api -Path '/sysAuthUser/deleteById' -Token $testToken -Body @{ id = 1 }
Write-Check 'test 删除用户被 403（按钮权限）' ($r.Http -eq 403) "http=$($r.Http)"

$r = Invoke-Api -Path '/sysAuthPermission/treeList' -Method GET -Token $testToken
Write-Check 'test 无权限管理菜单被 403' ($r.Http -eq 403) "http=$($r.Http)"

# ---------------- 06 注册 ----------------
Write-Host "--- 06 自助注册 ---" -ForegroundColor Yellow
$r = Invoke-Api -Path '/auth/register' -Body @{
    userName = $regUser; nickName = '注册用户'; password = $newPwd
    email = "$regUser@test.com"; phone = '13900139000'; sex = 0
}
Write-Check '自助注册成功' ($r.Code -eq 200) "code=$($r.Code)"

$r = Invoke-Api -Path '/auth/register' -Body @{ userName = $regUser; nickName = '重复'; password = $newPwd }
Write-Check '重复账号注册被拒绝' ($r.Http -eq 200 -and $r.Code -ne 200) "code=$($r.Code)"

$r = Invoke-Api -Path '/auth/login' -Body @{ username = $regUser; password = $newPwd }
Write-Check '注册用户可登录且仅有工作台菜单' ($r.Code -eq 200 -and $r.Data.user.menus.Count -eq 1) "code=$($r.Code)"
$regToken = $r.Data.token

# 注册用户（common 角色，仅工作台）：不应能拉取全量角色（该接口要求用户/角色/权限管理菜单权限之一）
$r = Invoke-Api -Path '/sysAuthRole/queryAll' -Method GET -Token $regToken
Write-Check '注册用户访问角色下拉被 403' ($r.Http -eq 403) "http=$($r.Http)"

# 反之，admin 与拥有用户管理菜单权限的 test 账号可正常拉取
$r = Invoke-Api -Path '/sysAuthRole/queryAll' -Method GET -Token $adminToken
Write-Check 'admin 可拉取角色下拉' ($r.Code -eq 200 -and @($r.Data).Count -ge 3) "code=$($r.Code)"
$r = Invoke-Api -Path '/sysAuthRole/queryAll' -Method GET -Token $testToken
Write-Check 'test（有用户管理菜单权限）可拉取角色下拉' ($r.Code -eq 200) "code=$($r.Code)"

# ---------------- 07 角色管理 ----------------
Write-Host "--- 07 角色管理 ---" -ForegroundColor Yellow
$demoRoleName = "t_角色_$ts"
$demoRoleKey  = "t_role_$ts"
$r = Invoke-Api -Path '/sysAuthRole/addAuthRole' -Token $adminToken -Body @{ roleName = $demoRoleName; roleKey = $demoRoleKey }
Write-Check 'admin 新增角色' ($r.Code -eq 200) "code=$($r.Code)"
$demoRoleId = [long]$r.Data

$r = Invoke-Api -Path '/sysAuthRole/paginQuery' -Token $adminToken -Body @{ pageNum = 1; pageSize = 10; roleKey = $demoRoleKey }
Write-Check '分页查询新角色' ($r.Code -eq 200 -and $r.Data.total -ge 1) "code=$($r.Code)"

$r = Invoke-Api -Path '/sysAuthRole/editAuthRole' -Token $adminToken -Body @{ id = $demoRoleId; roleName = $demoRoleName; roleKey = $demoRoleKey }
Write-Check 'admin 编辑角色' ($r.Code -eq 200) "code=$($r.Code)"

# 角色权限树
$r = Invoke-Api -Path '/sysAuthRolePermission/getPermissionByRoleId' -Token $adminToken -Body @{ roleId = $demoRoleId }
Write-Check '查询角色权限树（空授权）' ($r.Code -eq 200 -and $null -ne $r.Data.permissionTree) "code=$($r.Code)"

# 分配：用户管理菜单 + 列表按钮 + 新增按钮（不含删除）
$permTree = $r.Data.permissionTree
$permMenuUser = $null; $permBtnAdd = $null; $permBtnQuery = $null; $permBtnDel = $null
function Find-Node { param($nodes, $key) foreach ($n in $nodes) { if ($n.permissionKey -eq $key) { return $n }; if ($n.children) { $c = Find-Node $n.children $key; if ($c) { return $c } } } return $null }
$permMenuUser = Find-Node $permTree 'userInfo:userInfo'
$permBtnQuery = Find-Node $permTree 'sysAuthUser:paginQuery'
$permBtnAdd   = Find-Node $permTree 'sysAuthUser:addSysAuthUser'
$permBtnDel   = Find-Node $permTree 'sysAuthUser:deleteById'
Write-Check '权限树包含菜单与按钮节点' ($null -ne $permMenuUser -and $null -ne $permBtnAdd) ''

$r = Invoke-Api -Path '/sysAuthRole/assignPermission' -Token $adminToken -Body @{
    roleId = $demoRoleId
    permissionIds = @($permMenuUser.id, $permBtnQuery.id, $permBtnAdd.id)
}
Write-Check 'admin 为角色分配权限（菜单+查询+新增）' ($r.Code -eq 200) "code=$($r.Code)"

# 把 demo 角色分配给新用户（顶替 common，仅保留 demo）
$r = Invoke-Api -Path '/sysAuthUser/assignRole' -Token $adminToken -Body @{ userId = $newUserId; roleIds = @($demoRoleId) }
Write-Check '给用户切换为 demo 角色' ($r.Code -eq 200) "code=$($r.Code)"

# 重新登录（单用户单令牌：重新登录后旧令牌失效属预期行为），并验证新角色权限即时生效
$r = Invoke-Api -Path '/auth/login' -Body @{ username = $newUser; password = $newPwd }
Write-Check 'demo 用户重新登录获取新权限' ($r.Code -eq 200 -and ($r.Data.user.permissions -contains 'sysAuthUser:addSysAuthUser')) ''
$demoTok = $r.Data.token

$r = Invoke-Api -Path '/sysAuthUser/paginQuery' -Token $demoTok -Body @{ pageNum = 1; pageSize = 5 }
Write-Check 'demo 用户可查询用户列表（菜单权限）' ($r.Code -eq 200) "code=$($r.Code)"
$r = Invoke-Api -Path '/sysAuthUser/addSysAuthUser' -Token $demoTok -Body @{ userName = "t_x_$ts"; nickName = 'x'; password = '123456'; status = 0 }
Write-Check 'demo 用户可新增用户（按钮权限）' ($r.Code -eq 200) "code=$($r.Code)"
$r = Invoke-Api -Path '/sysAuthUser/deleteById' -Token $demoTok -Body @{ id = $newUserId }
Write-Check 'demo 用户无删除权限被 403' ($r.Http -eq 403) "http=$($r.Http)"

# ---------------- 08 内置保护规则 ----------------
Write-Host "--- 08 内置保护规则 ---" -ForegroundColor Yellow
$r = Invoke-Api -Path '/sysAuthUser/deleteById' -Token $adminToken -Body @{ id = 1 }
Write-Check '禁止删除内置管理员' ($r.Http -eq 200 -and $r.Code -ne 200) "code=$($r.Code)"
$r = Invoke-Api -Path '/sysAuthRole/deleteById' -Token $adminToken -Body @{ id = $roleAdmin.id }
Write-Check '禁止删除内置 admin 角色' ($r.Http -eq 200 -and $r.Code -ne 200) "code=$($r.Code)"
$r = Invoke-Api -Path '/sysAuthUser/assignRole' -Token $adminToken -Body @{ userId = 1; roleIds = @($roleCommon.id) }
Write-Check '禁止移除内置管理员的 admin 角色' ($r.Http -eq 200 -and $r.Code -ne 200) "http=$($r.Http) code=$($r.Code) msg=$($r.Message)"

# ---------------- 09 权限管理 CRUD ----------------
Write-Host "--- 09 权限管理 ---" -ForegroundColor Yellow
$tempKey = "sysAuthUser:t_demo_$ts"
$r = Invoke-Api -Path '/sysAuthPermission/addAuthPermission' -Token $adminToken -Body @{
    name = '测试按钮'; parentId = $permMenuUser.id; type = 1
    orderNum = 99; backendUrl = '/sysAuthUser/t_demo'; status = 0
    permissionKey = $tempKey
}
Write-Check 'admin 新增按钮权限' ($r.Code -eq 200) "code=$($r.Code)"
$tempPermId = [long]$r.Data

$r = Invoke-Api -Path '/sysAuthPermission/treeList' -Method GET -Token $adminToken
$found = $null -ne (Find-Node $r.Data $tempKey)
Write-Check '权限树包含新按钮' $found ''

$r = Invoke-Api -Path '/sysAuthPermission/editAuthPermission' -Token $adminToken -Body @{
    id = $tempPermId; name = '测试按钮-改'; parentId = $permMenuUser.id; type = 1
    orderNum = 99; backendUrl = '/sysAuthUser/t_demo'; status = 0; permissionKey = $tempKey
}
Write-Check 'admin 编辑按钮权限' ($r.Code -eq 200) "code=$($r.Code)"

$r = Invoke-Api -Path '/sysAuthPermission/deleteById' -Token $adminToken -Body @{ id = $tempPermId }
Write-Check 'admin 删除按钮权限' ($r.Code -eq 200) "code=$($r.Code)"

# ---------------- 10 重置密码 / 改密 / 令牌吊销 ----------------
Write-Host "--- 10 密码与令牌吊销 ---" -ForegroundColor Yellow
$newPwd2 = 'Passw0rd!2'
$r = Invoke-Api -Path '/sysAuthUser/resetPassword' -Token $adminToken -Body @{ userId = $newUserId; newPassword = $newPwd2 }
Write-Check 'admin 重置用户密码' ($r.Code -eq 200) "code=$($r.Code)"
$r = Invoke-Api -Path '/auth/getUserInfo' -Token $demoTok
Write-Check '重置后旧令牌立即失效（401）' ($r.Http -eq 401) "http=$($r.Http)"
$r = Invoke-Api -Path '/auth/login' -Body @{ username = $newUser; password = $newPwd2 }
Write-Check '新密码可登录' ($r.Code -eq 200) "code=$($r.Code)"
$demoToken = $r.Data.token

# 修改本人密码
$r = Invoke-Api -Path '/auth/updatePassword' -Token $demoToken -Body @{ oldPassword = $newPwd2; newPassword = $newPwd }
Write-Check '修改本人密码成功' ($r.Code -eq 200) "code=$($r.Code)"
$r = Invoke-Api -Path '/auth/getUserInfo' -Token $demoToken
Write-Check '改密后令牌失效（401）' ($r.Http -eq 401) "http=$($r.Http)"
$r = Invoke-Api -Path '/auth/login' -Body @{ username = $newUser; password = $newPwd }
Write-Check '原新密码可重新登录' ($r.Code -eq 200) "code=$($r.Code)"
$finalTok = $r.Data.token

# ---------------- 11 登出 ----------------
Write-Host "--- 11 登出 ---" -ForegroundColor Yellow
$r = Invoke-Api -Path '/auth/logout' -Token $finalTok
Write-Check '登出成功' ($r.Code -eq 200) "code=$($r.Code)"
$r = Invoke-Api -Path '/auth/getUserInfo' -Token $finalTok
Write-Check '登出后令牌失效（401）' ($r.Http -eq 401) "http=$($r.Http)"

# ---------------- 12 登录失败锁定 ----------------
Write-Host "--- 12 防暴力破解锁定 ---" -ForegroundColor Yellow
# 先建一个临时账号用于锁定测试
$r = Invoke-Api -Path '/sysAuthUser/addSysAuthUser' -Token $adminToken -Body @{
    userName = $lockUser; nickName = '锁定测试'; password = $newPwd; status = 0
}
Write-Check '创建锁定测试账号' ($r.Code -eq 200) "code=$($r.Code)"
$locked = $false
for ($i = 1; $i -le 5; $i++) {
    $r = Invoke-Api -Path '/auth/login' -Body @{ username = $lockUser; password = 'bad-pwd' }
}
$r = Invoke-Api -Path '/auth/login' -Body @{ username = $lockUser; password = $newPwd }
Write-Check '连续失败后正确密码也被锁定' ($r.Http -eq 200 -and $r.Code -eq 502) "code=$($r.Code) msg=$($r.Message)"

# ---------------- 13 清理测试数据 ----------------
Write-Host "--- 13 清理测试数据 ---" -ForegroundColor Yellow
# 删除 demo 角色需先移除用户关联
$r = Invoke-Api -Path '/sysAuthUser/assignRole' -Token $adminToken -Body @{ userId = $newUserId; roleIds = @($roleCommon.id) }
$r = Invoke-Api -Path '/sysAuthRole/deleteById' -Token $adminToken -Body @{ id = $demoRoleId }
Write-Check '清理 demo 角色' ($r.Code -eq 200) "code=$($r.Code)"

# 按账号前缀逐个逻辑删除测试用户（注册用户/锁定用户/其它临时用户）
$cleanupNames = @($newUser, $lockUser, $regUser, "t_x_$ts")
foreach ($name in $cleanupNames) {
    $r = Invoke-Api -Path '/sysAuthUser/paginQuery' -Token $adminToken -Body @{ pageNum = 1; pageSize = 20; userName = $name }
    foreach ($row in @($r.Data.records)) {
        if ($row.userName -eq $name) {
            $rr = Invoke-Api -Path '/sysAuthUser/deleteById' -Token $adminToken -Body @{ id = [long]$row.id }
            Write-Check "逻辑删除测试用户 $name" ($rr.Code -eq 200) "code=$($rr.Code)"
        }
    }
}

# ---------------- 14 菜单 status / show 字段生效性 ----------------
Write-Host "--- 14 菜单 status/show 生效 ---" -ForegroundColor Yellow
# 两个字段的约定语义（列注释 + 前端权限管理界面均一致）：
#   status：0 启用 / 1 禁用 —— 禁用后菜单不再下发，页面不可访问
#   show  ：0 展示 / 1 隐藏 —— 隐藏后菜单仍下发（可直接 URL 访问），由前端决定不在导航中显示
# 取「角色管理」菜单节点做验证，验证结束恢复原值。
$r = Invoke-Api -Path '/sysAuthPermission/treeList' -Method GET -Token $adminToken
$roleMenu = Find-Node $r.Data 'userInfo:roleInfo'
Write-Check '取到角色管理菜单节点' ($null -ne $roleMenu) ''

function Set-RoleMenuState {
    param([int]$Status, [int]$Show)
    return Invoke-Api -Path '/sysAuthPermission/editAuthPermission' -Token $adminToken -Body @{
        id = $roleMenu.id; name = $roleMenu.name; parentId = $roleMenu.parentId; type = 0
        orderNum = $roleMenu.orderNum; menuUrl = $roleMenu.menuUrl; component = $roleMenu.component
        backendUrl = $roleMenu.backendUrl; icon = $roleMenu.icon
        status = $Status; show = $Show; permissionKey = $roleMenu.permissionKey
    }
}

# 1) 禁用：不再下发
$r = Set-RoleMenuState -Status 1 -Show 0
Write-Check '禁用菜单保存成功' ($r.Code -eq 200) "code=$($r.Code)"
$r = Invoke-Api -Path '/auth/getUserInfo' -Token $adminToken
Write-Check '禁用菜单不再下发' ($null -eq (Find-Node $r.Data.menus 'userInfo:roleInfo')) ''

# 2) 恢复启用：重新下发
$r = Set-RoleMenuState -Status 0 -Show 0
$r = Invoke-Api -Path '/auth/getUserInfo' -Token $adminToken
Write-Check '恢复启用后菜单重新下发' ($null -ne (Find-Node $r.Data.menus 'userInfo:roleInfo')) ''

# 3) 隐藏：仍下发且 show=1（供前端隐藏导航、URL 直达）
$r = Set-RoleMenuState -Status 0 -Show 1
$r = Invoke-Api -Path '/auth/getUserInfo' -Token $adminToken
$hiddenMenu = Find-Node $r.Data.menus 'userInfo:roleInfo'
Write-Check '隐藏菜单仍下发（可 URL 直达）' ($null -ne $hiddenMenu) ''
Write-Check '隐藏菜单 show=1 已下发' ($null -ne $hiddenMenu -and $hiddenMenu.show -eq 1) "show=$($hiddenMenu.show)"

# 4) 复原为展示
$r = Set-RoleMenuState -Status 0 -Show 0
$r = Invoke-Api -Path '/auth/getUserInfo' -Token $adminToken
$restoredMenu = Find-Node $r.Data.menus 'userInfo:roleInfo'
Write-Check '复原展示状态' ($null -ne $restoredMenu -and $restoredMenu.show -eq 0) "show=$($restoredMenu.show)"

# ---------------- 15 列表排序白名单 ----------------
Write-Host "--- 15 列表排序白名单 ---" -ForegroundColor Yellow
# 合法字段：按账号升序，服务端经白名单映射为 user_name ASC
$r = Invoke-Api -Path '/sysAuthUser/paginQuery' -Token $adminToken -Body @{
    pageNum = 1; pageSize = 10; sortField = 'userName'; sortOrder = 'asc'
}
$names = @($r.Data.records | ForEach-Object { $_.userName })
$sortedAsc = $true
for ($i = 1; $i -lt $names.Count; $i++) {
    if ([string]::CompareOrdinal($names[$i - 1], $names[$i]) -gt 0) { $sortedAsc = $false; break }
}
Write-Check '用户列表按账号升序生效' ($r.Code -eq 200 -and $names.Count -ge 2 -and $sortedAsc) "names=$($names -join ',')"

# 非法字段：应被忽略（不报错、不回显列名），返回默认排序结果
$rDefault = Invoke-Api -Path '/sysAuthUser/paginQuery' -Token $adminToken -Body @{ pageNum = 1; pageSize = 10 }
$rUnknown = Invoke-Api -Path '/sysAuthUser/paginQuery' -Token $adminToken -Body @{
    pageNum = 1; pageSize = 10; sortField = 'notAColumn'; sortOrder = 'asc'
}
$defaultFirst = @($rDefault.Data.records)[0].userId
$unknownFirst = @($rUnknown.Data.records)[0].userId
Write-Check '未登记排序字段被忽略（回落默认排序）' ($rUnknown.Code -eq 200 -and $unknownFirst -eq $defaultFirst) "default=$defaultFirst unknown=$unknownFirst"

# 角色列表：按角色名降序
$r = Invoke-Api -Path '/sysAuthRole/paginQuery' -Token $adminToken -Body @{
    pageNum = 1; pageSize = 10; sortField = 'roleName'; sortOrder = 'desc'
}
$roleNames = @($r.Data.records | ForEach-Object { $_.roleName })
$sortedDesc = $true
for ($i = 1; $i -lt $roleNames.Count; $i++) {
    if ([string]::CompareOrdinal($roleNames[$i - 1], $roleNames[$i]) -lt 0) { $sortedDesc = $false; break }
}
Write-Check '角色列表按角色名降序生效' ($r.Code -eq 200 -and $roleNames.Count -ge 2 -and $sortedDesc) "roleNames=$($roleNames -join ',')"

# 排序字段含非法字符（空格）：参数校验阶段被拒绝，进不到 SQL 拼装
$r = Invoke-Api -Path '/sysAuthUser/paginQuery' -Token $adminToken -Body @{
    pageNum = 1; pageSize = 10; sortField = 'user name'
}
Write-Check '排序字段含非法字符被参数校验拒绝' ($r.Code -eq 400) "code=$($r.Code) msg=$($r.Message)"

# ---------------- 16 校验分组（新增/编辑必填差异） ----------------
Write-Host "--- 16 校验分组 ---" -ForegroundColor Yellow
# 1) 编辑缺 id：应由参数校验拦下并给出明确提示（而不是业务层的"数据不存在"）
$r = Invoke-Api -Path '/sysAuthRole/editAuthRole' -Token $adminToken -Body @{ roleName = '缺ID角色'; roleKey = "t_noid_$ts" }
Write-Check '编辑缺 id 返回参数校验错误' ($r.Code -eq 400 -and $r.Message -like '*ID不能为空*') "code=$($r.Code) msg=$($r.Message)"

# 2) 编辑带 id 但必填字段为空：默认组约束仍须生效（验证分组继承了 Default）
$r = Invoke-Api -Path '/sysAuthRole/editAuthRole' -Token $adminToken -Body @{ id = 2; roleName = ''; roleKey = 'cehsi1' }
Write-Check '编辑字段为空仍被默认约束拦截' ($r.Code -eq 400) "code=$($r.Code) msg=$($r.Message)"

# 3) 新增缺必填字段：Create 分组同样继承默认组约束
$r = Invoke-Api -Path '/sysAuthRole/addAuthRole' -Token $adminToken -Body @{ roleName = ''; roleKey = "t_bad_$ts" }
Write-Check '新增字段为空被默认约束拦截' ($r.Code -eq 400) "code=$($r.Code) msg=$($r.Message)"

# 4) 权限编辑缺 id：同一分组机制对权限接口同样生效
$r = Invoke-Api -Path '/sysAuthPermission/editAuthPermission' -Token $adminToken -Body @{ name = '缺ID权限'; type = 1; permissionKey = "sysAuthUser:t_noid_$ts" }
Write-Check '权限编辑缺 id 返回参数校验错误' ($r.Code -eq 400 -and $r.Message -like '*ID不能为空*') "code=$($r.Code) msg=$($r.Message)"

# ---------------- 汇总 ----------------
Write-Host ""
Write-Host "========== 测试汇总 ==========" -ForegroundColor Cyan
Write-Host "通过: $($script:Pass)   失败: $($script:Fail)"
if ($script:Fail -gt 0) {
    Write-Host "失败明细:" -ForegroundColor Red
    $script:Failures | ForEach-Object { Write-Host "  - $_" -ForegroundColor Red }
}
