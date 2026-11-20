.PHONY: help up down logs ps db-reset test backend-test docs-test

help:
	@echo "make up         启动 MySQL/Redis/后端/桌面端/移动端"
	@echo "make down       停止并保留数据卷"
	@echo "make logs       跟踪全部容器日志"
	@echo "make db-reset   删除数据卷并重新初始化（会清空本地数据）"
	@echo "make test       后端单测 + 文档/权限检查"

up:
	cp -n .env.example .env || true
	docker compose up -d --build

down:
	docker compose down

logs:
	docker compose logs -f

ps:
	docker compose ps

db-reset:
	docker compose down -v
	docker compose up -d --build

backend-test:
	mvn -f admintem-backend/pom.xml test

docs-test:
	node admintem-docs/tests/check-doc-images.cjs
	node admintem-docs/tests/check-tokens.cjs
	node admintem-docs/tests/check-perm-keys.cjs
	node admintem-docs/tests/check-permission-doc-consistency.cjs
	node admintem-docs/tests/gen-role-endpoint-matrix.cjs --check

test: backend-test docs-test
