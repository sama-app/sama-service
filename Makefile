# App
build:
	@mvn --batch-mode clean install -Dspring.profiles.active=ci -pl app

verify:
	@mvn --batch-mode verify -Dspring.profiles.active=ci -pl app

container: build
	docker pull 216862985054.dkr.ecr.eu-central-1.amazonaws.com/sama-service:latest || true
	docker build -t sama-service .

upload-to-ecr:
	docker tag sama-service:latest 216862985054.dkr.ecr.eu-central-1.amazonaws.com/sama-service:latest
	docker push 216862985054.dkr.ecr.eu-central-1.amazonaws.com/sama-service:latest

# Deployment
terraform-init:
	@terraform -chdir=terraform init -input=false

terraform-validate:
	terraform -chdir=terraform fmt -check

dev-current-deployment:
	@terraform -chdir=terraform show -json | \
	jq -r '.values.root_module.resources[] | select (.type == "aws_lb_listener_rule") | .values.action[].forward[].target_group[] | select (.weight == 100) | .arn' | \
	grep -o -P '(?<=sama-service-).*(?=-tg-dev)'

dev-deploy-green:
	terraform -chdir=terraform apply -auto-approve \
		-var 'enable_green_env=true' \
		-var 'enable_blue_env=false' \
		-var 'traffic_distribution=green'

dev-deploy-blue:
	terraform -chdir=terraform apply -auto-approve \
		-var 'enable_green_env=false' \
		-var 'enable_blue_env=true' \
		-var 'traffic_distribution=blue'


# Local development
container-run:
	docker run -d \
          --name sama-service \
          -p 3000:3000 \
          -v /var/log/sama/sama-service:/var/log/sama/sama-service \
          sama-service:latest

purge-db:
	mvn liquibase:rollback -Dliquibase.rollbackCount=9999 -pl app

rollback-one:
	mvn liquibase:rollback -Dliquibase.rollbackCount=1 -pl app
