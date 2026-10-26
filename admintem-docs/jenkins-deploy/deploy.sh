#!/usr/bin/env bash
# /opt/admin-demo/deploy.sh
# 由 Jenkins 自由风格任务通过 SSH 调用；构建和运行都使用目标机现有 JDK21 / Node / Nginx。
set -euo pipefail

BASE=/opt/admin-demo
REPO=https://gitee.com/liuwei9599/admin-template.git
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export JAVA_HOME
export PATH="$JAVA_HOME/bin:/usr/local/bin:/usr/bin:/bin"

echo "===== [1/8] 同步代码 ====="
mkdir -p "$BASE/src"
cd "$BASE/src"
if [ -d .git ]; then
  git fetch origin master
  git reset --hard origin/master
else
  git clone --depth 1 "$REPO" .
fi
git --no-pager log -1 --format='commit=%h %s'

echo "===== [2/8] 构建后端（JDK21，target 17） ====="
cd "$BASE/src/admintem-backend"
mvn -B -Pdev -DskipTests clean package
JAR="$BASE/src/admintem-backend/target/admintem-backend.jar"
test -f "$JAR"
mkdir -p "$BASE/backend" "$BASE/logs" "$BASE/run" "$BASE/config"
cp "$JAR" "$BASE/backend/admintem-backend.jar.new"

echo "===== [3/8] 构建桌面端（Vue） ====="
cd "$BASE/src/admintem-vue"
npm ci
npm run build
rm -rf "$BASE/frontend.new"
cp -a dist "$BASE/frontend.new"

echo "===== [4/8] 构建移动端（Vant，挂载 /mobile/） ====="
cd "$BASE/src/admintem-mobile"
npm ci
npm run build -- --base=/mobile/
mkdir -p "$BASE/frontend.new/mobile"
cp -a dist/. "$BASE/frontend.new/mobile/"

echo "===== [5/8] 替换静态文件 ====="
rm -rf "$BASE/frontend"
mv "$BASE/frontend.new" "$BASE/frontend"
mv "$BASE/backend/admintem-backend.jar.new" "$BASE/backend/admintem-backend.jar"
mkdir -p "$BASE/uploads/avatar"

echo "===== [6/8] 重启后端 ====="
if [ -f "$BASE/run/backend.pid" ]; then
  OLD_PID=$(cat "$BASE/run/backend.pid" || true)
  if [ -n "${OLD_PID:-}" ] && kill -0 "$OLD_PID" 2>/dev/null; then
    kill "$OLD_PID" || true
    for _ in $(seq 1 20); do
      kill -0 "$OLD_PID" 2>/dev/null || break
      sleep 1
    done
  fi
fi
cd "$BASE"
nohup "$JAVA_HOME/bin/java" \
  -XX:MaxRAMPercentage=60 -Dfile.encoding=UTF-8 \
  -jar "$BASE/backend/admintem-backend.jar" \
  --spring.profiles.active=dev \
  --server.port=8090 \
  --admintem.security.cors-allowed-origins='http://localhost:*,http://127.0.0.1:*,http://192.168.1.167:8092' \
  > "$BASE/logs/backend.out" 2>&1 &
echo $! > "$BASE/run/backend.pid"
echo "backend pid=$(cat "$BASE/run/backend.pid")"

echo "===== [7/8] 等待健康检查 ====="
HEALTH=DOWN
for _ in $(seq 1 60); do
  if curl -fsS http://127.0.0.1:8090/actuator/health 2>/dev/null | grep -q '"status":"UP"'; then
    HEALTH=UP
    break
  fi
  sleep 2
done
if [ "$HEALTH" != "UP" ]; then
  echo "后端健康检查失败，最近日志："
  tail -80 "$BASE/logs/backend.out" || true
  exit 1
fi
echo "backend health=UP"

echo "===== [8/8] 启动/重载独立 Nginx（8092） ====="
mkdir -p "$BASE/nginx" "$BASE/logs"
# nginx -t 或上次失败可能留下空的 pid；只有 pid 非空且进程存活才 reload
if [ -s "$BASE/nginx/nginx.pid" ] && kill -0 "$(cat "$BASE/nginx/nginx.pid")" 2>/dev/null; then
  /usr/sbin/nginx -p "$BASE/nginx" -c "$BASE/nginx/nginx.conf" -s reload
else
  rm -f "$BASE/nginx/nginx.pid"
  /usr/sbin/nginx -p "$BASE/nginx" -c "$BASE/nginx/nginx.conf"
fi
curl -fsS -o /dev/null -w 'frontend http=%{http_code}\n' http://127.0.0.1:8092/
echo "===== 部署完成: http://192.168.1.167:8092 ====="
