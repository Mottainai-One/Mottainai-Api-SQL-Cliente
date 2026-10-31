# Mottainai API SQL Cliente

API do ecossistema Mottainai destinada ao aplicativo Android do consumidor. O projeto pretende oferecer acesso autorizado ao perfil, catálogo, fidelidade e recursos de localização por meio de serviços HTTP e do banco operacional compartilhado.

## Visão de arquitetura

O aplicativo cliente se integra aos serviços HTTP do ecossistema. O banco operacional e sua configuração são mantidos separadamente.

## Tecnologias

- Java 21.
- Spring Boot 4.0.0 e Spring MVC, declarados em [pom.xml](pom.xml).
- Maven para build e testes.
- JUnit e ferramentas de teste do Spring Boot no escopo de testes.

## Estrutura

```text
.
├── pom.xml
├── src/main/java/com/institutojf/mottainai/client/
│   └── MottainaiClientApiApplication.java
├── src/main/resources/application.yaml
├── src/test/java/com/institutojf/mottainai/client/
│   └── MottainaiClientApiApplicationTests.java
└── LICENSE
```

## Requisitos e execução

Instale Java 21 e Maven 3.9 ou superior. Na raiz do repositório:

```bash
mvn test
mvn spring-boot:run
```

Esses comandos executam o teste de inicialização e iniciam a estrutura Spring Boot. A configuração versionada em `application.yaml` define o nome da aplicação; a branch principal não fornece um modelo de variáveis de ambiente nem integrações externas configuradas.

## Funcionalidades e contratos

Os contratos de perfil, catálogo, fidelidade, geofencing e autenticação estão descritos no [guia de rotas e exemplos da branch de documentação](https://github.com/Mottainai-One/Mottainai-Api-SQL-Cliente/blob/docs/customer-api-usage/docs/api-usage.md). Consulte a branch correspondente para utilizar esses contratos.

O schema, funções, views, políticas RLS e credenciais do PostgreSQL pertencem ao ambiente de banco e devem ser provisionados separadamente quando a integração estiver disponível. Firebase e recuperação de senha devem ser validados contra a versão efetivamente implantada.

## Testes

O teste versionado verifica a inicialização do contexto da aplicação. Ele não valida endpoints, banco, Firebase ou fluxos do aplicativo cliente. Não há Swagger nem script de execução Swagger na árvore atual de `main`.

## Contribuição e licença

Abra uma pull request com a funcionalidade, seus contratos e evidências de teste. Atualize este README quando as branches de negócio forem incorporadas à principal.

Licença: [MIT](LICENSE).
