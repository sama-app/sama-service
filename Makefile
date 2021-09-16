###########
### App ###
###########
app-build:
	@mvn --batch-mode -T 1C install -DskipTests -DskipITs -Dspring.profiles.active=ci -pl app

app-test:
	@mvn --batch-mode -T 1C surefire:test -DskipITs

app-integration-test:
	@mvn --batch-mode -T 1C failsafe:integration-test
	@mvn --batch-mode -T 1C failsafe:verify

app-verify: app-test app-integration-test

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

app-tag-ecr-image:
	$(MAKE) tag-ecr-image IMAGE=sama-service TAG=$(BUILD_VERSION) NEW_TAG=$(VERSION)

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

webserver-tag-ecr-image:
	$(MAKE) tag-ecr-image IMAGE=sama-webserver TAG=$(BUILD_VERSION) NEW_TAG=$(VERSION)

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

tag-ecr-image:
	@echo "Tagging $(IMAGE):$(TAG) with $(NEW_TAG)"
	@aws ecr batch-get-image --repository-name $(IMAGE) --image-ids imageTag=$(TAG) --query 'images[].imageManifest' --output text > manifest.json
	@aws ecr put-image --repository-name $(IMAGE) --image-tag $(NEW_TAG) --image-manifest file://manifest.json
	@rm manifest.json

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
liquibase-run:
	$(MAKE) liquibase-$(ENV) CMD='update'

liquibase-rollback:
	$(MAKE) liquibase-$(ENV) CMD='rollback -Dliquibase.rollbackCount=1'

liquibase-local:
	$(MAKE) liquibase-cmd ENV=local

liquibase-dev:
	@ssh -i /home/balys/.ssh/sama-dev.pem -fT -L 15432:sama-dev.cp9s2aovpufd.eu-central-1.rds.amazonaws.com:5432 ubuntu@3.68.150.223 sleep 30
	$(MAKE) liquibase-cmd ENV=dev

liquibase-prod:
	@ssh -i /home/balys/.ssh/sama-dev.pem -fT -L 15432:sama-prod.cp9s2aovpufd.eu-central-1.rds.amazonaws.com:5432 ubuntu@18.198.25.70 sleep 30
	$(MAKE) liquibase-cmd ENV=prod

liquibase-cmd:
	@mvn liquibase:$(CMD) -Denv=$(ENV) -pl app
