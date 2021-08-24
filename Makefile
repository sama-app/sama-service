###########
### App ###
###########
app-build:
	@mvn --batch-mode clean install -Dspring.profiles.active=ci -pl app

app-verify:
	@mvn --batch-mode verify -Dspring.profiles.active=ci -pl app

app-container: app-build
	docker pull 216862985054.dkr.ecr.eu-central-1.amazonaws.com/sama-service:latest || true
	docker build -t sama-service app/

app-upload-to-ecr:
	docker tag sama-service:latest 216862985054.dkr.ecr.eu-central-1.amazonaws.com/sama-service:$(VERSION)
	docker push 216862985054.dkr.ecr.eu-central-1.amazonaws.com/sama-service:$(VERSION)

#################
### Webserver ###
#################
webserver-container:
	docker pull 216862985054.dkr.ecr.eu-central-1.amazonaws.com/sama-webserver:latest || true
	docker build -t sama-webserver webserver/

webserver-upload-to-ecr:
	docker tag sama-webserver:latest 216862985054.dkr.ecr.eu-central-1.amazonaws.com/sama-webserver:$(VERSION)
	docker push 216862985054.dkr.ecr.eu-central-1.amazonaws.com/sama-webserver:$(VERSION)

##################
### Deployment ###
##################
terraform-init: terraform-init
	@terraform -chdir=infra/terraform init -input=false
	@terraform -chdir=infra/terraform workspace new $(ENV) ||:

terraform-validate:
	terraform -chdir=infra/terraform fmt -check

current-deployment:
	@terraform -chdir=infra/terraform workspace select $(ENV) > /dev/null
	@terraform -chdir=infra/terraform show -json | \
	jq -r '.values.root_module.resources[] | select (.type == "aws_lb_listener_rule") | .values.action[].forward[].target_group[] | select (.weight == 100) | .arn' | \
	grep -o -P '(?<=sama-service-).*(?=-tg-$(ENV))'

deploy:
	@terraform -chdir=infra/terraform workspace select $(ENV)
	terraform -chdir=infra/terraform apply -auto-approve \
		-var 'enable_green_env=true' \
		-var 'enable_blue_env=true' \
		-var 'traffic_distribution=split'

destroy-blue:
	@terraform -chdir=infra/terraform workspace select $(ENV)
	terraform -chdir=infra/terraform apply -auto-approve \
		-var 'enable_green_env=true' \
		-var 'enable_blue_env=false' \
		-var 'traffic_distribution=green'

destroy-green:
	@terraform -chdir=infra/terraform workspace select $(ENV)
	terraform -chdir=infra/terraform apply -auto-approve \
		-var 'enable_green_env=false' \
		-var 'enable_blue_env=true' \
		-var 'traffic_distribution=blue'

purge-db:
	mvn liquibase:rollback -Dliquibase.rollbackCount=9999 -pl app

rollback-one:
	mvn liquibase:rollback -Dliquibase.rollbackCount=1 -pl app
