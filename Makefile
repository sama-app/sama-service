build:
	mvn clean install

build-container: build
	docker build -t sama-service .

upload-to-ecr:
	docker tag sama-service:latest 216862985054.dkr.ecr.eu-central-1.amazonaws.com/sama-service:latest
	docker push 216862985054.dkr.ecr.eu-central-1.amazonaws.com/sama-service:latest

deploy:
	ssh ubuntu@18.193.45.91 -i ~/.ssh/sama-dev.pem ./deploy.sh

aws: build-container upload-to-ecr deploy

purge-db:
	mvn liquibase:rollback -Dliquibase.rollbackCount=9999

rollback-one:
		mvn liquibase:rollback -Dliquibase.rollbackCount=1


https://app.yoursama.com/api/auth/success?accessToken=