###########
### App ###
###########
app-build:
	@mvn --batch-mode clean install -DskipITs -Dspring.profiles.active=ci -pl app

app-verify:
	@mvn --batch-mode verify -Dspring.profiles.active=ci -pl app

app-container: app-build
	$(MAKE) container \
		IMAGE_URI=216862985054.dkr.ecr.eu-central-1.amazonaws.com/sama-service \
		IMAGE=sama-service \
		SOURCE=app/

app-upload-to-ecr:
	$(MAKE) upload-to-ecr \
		IMAGE_URI=216862985054.dkr.ecr.eu-central-1.amazonaws.com/sama-service \
		IMAGE=sama-service \
		VERSION=$(VERSION)

app-pull-image:
	$(MAKE) pull-image \
		IMAGE_URI=216862985054.dkr.ecr.eu-central-1.amazonaws.com/sama-service \
		IMAGE=sama-service \
		BUILD_VERSION=$(BUILD_VERSION)

#################
### Webserver ###
#################
webserver-container:
	$(MAKE) container \
		IMAGE_URI=216862985054.dkr.ecr.eu-central-1.amazonaws.com/sama-webserver \
		IMAGE=sama-webserver \
		SOURCE=webserver/

webserver-upload-to-ecr:
	$(MAKE) upload-to-ecr \
		IMAGE_URI=216862985054.dkr.ecr.eu-central-1.amazonaws.com/sama-webserver \
		IMAGE=sama-webserver \
		VERSION=$(VERSION)

webserver-pull-image:
	$(MAKE) pull-image \
		IMAGE_URI=216862985054.dkr.ecr.eu-central-1.amazonaws.com/sama-webserver \
		IMAGE=sama-webserver \
		BUILD_VERSION=$(BUILD_VERSION)


##################
### Versioning ###
##################
version:
	@echo -n "b-" && git rev-parse --short HEAD

##############
### Docker ###
##############
container:
	docker pull $(IMAGE_URI):latest || true
	docker build -t $(IMAGE) $(SOURCE)

pull-image:
	docker pull $(IMAGE_URI):$(BUILD_VERSION)
	docker tag $(IMAGE_URI):$(BUILD_VERSION) $(IMAGE):latest

upload-to-ecr:
	docker tag $(IMAGE):latest $(IMAGE_URI):$(VERSION)
	docker push $(IMAGE_URI):$(VERSION)

######################
### Deployment ECS ###
######################
ecs-terraform-init:
	@terraform -chdir=infra/ecs/terraform init -input=false
	@terraform -chdir=infra/ecs/terraform workspace new $(ENV) ||:

ecs-terraform-validate:
	terraform -chdir=infra/ecs/terraform fmt -check

ecs-update-infra:
	@terraform -chdir=infra/ecs/terraform workspace select $(ENV)
	terraform -chdir=infra/ecs/terraform apply -auto-approve

ecs-pull-task-definition:
	aws ecs describe-task-definition \
		--task-definition sama-service-$(ENV) \
		--query taskDefinition > task-definition.json

######################
### Deployment EC2 ###
######################
terraform-init:
	@terraform -chdir=infra/ec2/terraform init -input=false
	@terraform -chdir=infra/ec2/terraform workspace new $(ENV) ||:

terraform-validate:
	terraform -chdir=infra/ec2/terraform fmt -check

current-deployment:
	@terraform -chdir=infra/ec2/terraform workspace select $(ENV) > /dev/null
	@terraform -chdir=infra/ec2/terraform show -json | \
	jq -r '.values.root_module.resources[] | select (.type == "aws_lb_listener_rule") | .values.action[].forward[].target_group[] | select (.weight == 100) | .arn' | \
	grep -o -P '(?<=sama-service-).*(?=-tg-$(ENV))'

deploy:
	@terraform -chdir=infra/ec2/terraform workspace select $(ENV)
	terraform -chdir=infra/ec2/terraform apply -auto-approve \
		-var 'enable_green_env=true' \
		-var 'enable_blue_env=true' \
		-var 'traffic_distribution=split'

destroy-blue:
	@terraform -chdir=infra/ec2/terraform workspace select $(ENV)
	terraform -chdir=infra/ec2/terraform apply -auto-approve \
		-var 'enable_green_env=true' \
		-var 'enable_blue_env=false' \
		-var 'traffic_distribution=green'

destroy-green:
	@terraform -chdir=infra/ec2/terraform workspace select $(ENV)
	terraform -chdir=infra/ec2/terraform apply -auto-approve \
		-var 'enable_green_env=false' \
		-var 'enable_blue_env=true' \
		-var 'traffic_distribution=blue'

#################
### Liquibase ###
#################
purge-db:
	mvn liquibase:rollback -Dliquibase.rollbackCount=9999 -pl app

rollback-one:
	mvn liquibase:rollback -Dliquibase.rollbackCount=1 -pl app
