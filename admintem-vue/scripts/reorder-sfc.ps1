# 把各 .vue 顶层块重排为规范顺序：<template> → <script> → <style>
# 仅对 script 出现在 template 之前的文件生效；以“顶格标签”识别顶层块，
# 内部 <template #xxx> 等均有缩进，不会被误判。

$vueDir = 'E:\frameworkCaseCode\lc-2025-work\admintem-work-new\admintem-vue\src'
$files = Get-ChildItem $vueDir -Recurse -Filter *.vue
$enc = New-Object System.Text.UTF8Encoding($false)
$changed = 0

foreach ($f in $files) {
    $lines = [System.IO.File]::ReadAllLines($f.FullName, [System.Text.Encoding]::UTF8)
    # 定位顶层块
    $blocks = @()   # @{type; start; end} 行号从 0 开始
    $i = 0
    while ($i -lt $lines.Length) {
        $line = $lines[$i]
        $type = $null
        if ($line -match '^<template>$') { $type = 'template' }
        elseif ($line -match '^<script setup>$') { $type = 'script' }
        elseif ($line -match '^<script>$') { $type = 'script' }
        elseif ($line -match '^<style.*>$') { $type = 'style' }
        if ($type) {
            $close = "</$type>"
            $end = -1
            for ($j = $i + 1; $j -lt $lines.Length; $j++) {
                if ($lines[$j] -eq $close) { $end = $j; break }
            }
            if ($end -lt 0) { Write-Warning "未闭合块 $type in $($f.Name)"; break }
            $blocks += [pscustomobject]@{ type = $type; start = $i; end = $end }
            $i = $end + 1
        } else {
            $i++
        }
    }
    if ($blocks.Count -eq 0) { continue }
    # 记录原始顺序：script 是否排在 template 前
    $t = ($blocks | Where-Object type -eq 'template' | Select-Object -First 1)
    $s = ($blocks | Where-Object type -eq 'script'   | Select-Object -First 1)
    if (-not $t -or -not $s) { continue }
    if ($s.start -le $t.start) {
        $order = @('template','script','style')
        $out = New-Object System.Collections.Generic.List[string]
        $consumed = 0
        foreach ($typ in $order) {
            $blk = $blocks | Where-Object type -eq $typ | Select-Object -First 1
            if ($blk) {
                for ($k = $blk.start; $k -le $blk.end; $k++) { $out.Add($lines[$k]) }
                $out.Add('')
                $consumed += ($blk.end - $blk.start + 1)
            }
        }
        # 校验没有丢行（除纯空行/首尾空白）
        $totalConsumable = ($lines | Where-Object { $_.Trim() -ne '' }).Count
        $consumedNonBlank = ($out | Where-Object { $_.Trim() -ne '' }).Count
        if ($consumedNonBlank -ne $totalConsumable) {
            Write-Warning "行数不一致，跳过 $($f.Name): expect=$totalConsumable got=$consumedNonBlank"
            continue
        }
        [System.IO.File]::WriteAllLines($f.FullName, $out.ToArray(), $enc)
        Write-Output "reordered: $($f.FullName.Replace($vueDir+'\',''))"
        $changed++
    }
}
Write-Output "changed=$changed"
