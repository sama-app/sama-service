build:
	@mvn --batch-mode clean install -Dspring.profiles.active=ci

verify:
	@mvn --batch-mode verify -Dspring.profiles.active=ci

container: build
	docker pull 216862985054.dkr.ecr.eu-central-1.amazonaws.com/sama-service:latest || true
	docker build -t sama-service .

container-run:
	docker run -d \
          --name sama-service \
          -p 3000:3000 \
          -v /var/log/sama/sama-service:/var/log/sama/sama-service \
          sama-service:latest

upload-to-ecr:
	docker tag sama-service:latest 216862985054.dkr.ecr.eu-central-1.amazonaws.com/sama-service:latest
	docker push 216862985054.dkr.ecr.eu-central-1.amazonaws.com/sama-service:latest

purge-db:
	mvn liquibase:rollback -Dliquibase.rollbackCount=9999

rollback-one:
	mvn liquibase:rollback -Dliquibase.rollbackCount=1
