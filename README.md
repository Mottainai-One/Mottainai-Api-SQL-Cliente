# Mottainai API SQL Cliente

API do ecossistema Mottainai destinada ao aplicativo Android do consumidor. O projeto pretende oferecer acesso autorizado ao perfil, catálogo, fidelidade e recursos de localização por meio de serviços HTTP e do banco operacional compartilhado.

## Estado da branch principal

Na versão de `main` analisada, este repositório contém a **estrutura inicial** de uma aplicação Spring Boot: classe de inicialização, configuração do nome da aplicação e teste de inicialização. Não há controllers de negócio, acesso PostgreSQL, configuração Firebase, Spring Security ou dependência OpenAPI nesse código.

O README anterior da branch descrevia rotas e integrações que **não estão presentes nessa versão**. Há trabalho de API e documentação em outras branches; seu conteúdo e disponibilidade precisam ser conferidos na branch escolhida antes de usar seus contratos.

## Visão de arquitetura

O aplicativo cliente é o consumidor previsto da API. O desenho pretendido usa autenticação e uma API para alcançar dados do banco operacional, que é administrado separadamente. A estrutura disponível em `main` ainda não implementa esse fluxo; não há confirmação de integração ou homologação ponta a ponta nesta documentação.

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

## Contratos e trabalho em outras branches

O trabalho de perfil, catálogo, fidelidade, geofencing e autenticação aparece em branches de desenvolvimento. Um guia de rotas e exemplos está disponível na [branch de documentação da API](https://github.com/Mottainai-One/Mottainai-Api-SQL-Cliente/blob/docs/customer-api-usage/docs/api-usage.md). Essa referência documenta outra branch e não comprova que seus endpoints estejam na `main` ou implantados.

O schema, funções, views, políticas RLS e credenciais do PostgreSQL pertencem ao ambiente de banco e devem ser provisionados separadamente quando a integração estiver disponível. Firebase e recuperação de senha devem ser validados contra a versão efetivamente implantada.

## Testes e limitações

O teste versionado verifica a inicialização do contexto da aplicação. Ele não valida endpoints, banco, Firebase ou fluxos do aplicativo cliente. Não há Swagger nem script de execução Swagger na árvore atual de `main`.

## Contribuição e licença

Abra uma pull request com a funcionalidade, seus contratos e evidências de teste. Atualize este README quando as branches de negócio forem incorporadas à principal.

Licença: [MIT](LICENSE).
