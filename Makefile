.PHONY: neo4j backend frontend test-backend test-frontend clean

neo4j:
	docker compose up -d neo4j

backend:
	cd backend && mvn spring-boot:run

frontend:
	cd frontend && npm install && npm run dev

test-backend:
	cd backend && mvn test

test-frontend:
	cd frontend && npm install && npm run build

clean:
	rm -rf backend/target frontend/node_modules frontend/dist frontend/build target node_modules dist build __MACOSX
