build:
	./mvnw clean install -DskipTests

container: build
	docker build -t sama-service .

upload-to-ecr:
	docker tag sama-service:latest 216862985054.dkr.ecr.eu-central-1.amazonaws.com/sama-service:latest
	docker push 216862985054.dkr.ecr.eu-central-1.amazonaws.com/sama-service:latest

purge-db:
	mvn liquibase:rollback -Dliquibase.rollbackCount=9999

rollback-one:
	mvn liquibase:rollback -Dliquibase.rollbackCount=1
