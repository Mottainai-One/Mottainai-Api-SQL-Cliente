# Avisos de promoções do Mobile Cliente

## Fluxo implementado

A API Cliente consulta o catálogo aprovado e prepara a fila transacional no PostgreSQL. Um produtor agendado publica cada entrega no endpoint privado da API Redis. O Redis armazena a caixa individual; o Android usa uma credencial curta para consultar somente os próprios avisos. Firebase permanece responsável apenas pela autenticação.

Não é necessário alterar a API Operacional nem a IA para esse produtor. Não há FCM, Firestore, envio de senhas ou segredos privados ao aplicativo.

## Banco e regras de negócio

Aplicar primeiro `database/operational/42_customer_promotion_notifications.sql`, do repositório do banco, após as migrações anteriores. Essa migração cria preferências, campanhas e entregas, índices, funções e permissões RLS. Não altera compras nem o cálculo de pontos.

- Raio pessoal de 5 ou 10 km, a partir da posição real consentida.
- Posição recebida com até dois minutos de idade e até 30 segundos de tolerância no futuro; vence 30 minutos após a observação. Não há histórico de deslocamento.
- Clientes ativos, com adesão explícita e posição válida; lojas ativas e promoções aprovadas, ativas, vigentes, com itens disponíveis.
- Até três produtos distintos por loja/dia, no fuso America/Sao_Paulo, selecionando os maiores descontos percentuais.
- Os primeiros `ceil(clientes próximos × 0,20)`, ordenados por pontos decrescentes, recebem liberação imediata. Empates usam o ID do cliente. Os demais têm liberação 30 minutos depois da criação da campanha.
- A elegibilidade é reavaliada na reserva de cada entrega. Uma campanha pode voltar a participar no dia seguinte, respeitando a quota diária.
- A reserva tem lease de 90 segundos; falhas recebem nova tentativa com backoff. A publicação usa um ID estável, preservando a deduplicação no Redis.

O horário acima representa a liberação no servidor. A apresentação no telefone depende de rede, permissões e agendamento do Android, não sendo uma garantia de push instantâneo.

## Rotas de preferência

Todas exigem `Authorization: Bearer <Firebase-ID-token>`. A identidade vem exclusivamente do token, nunca de um customerId enviado pelo aparelho.

| Método e rota | Contrato |
|---|---|
| GET /api/v1/client/notification-preferences | Consulta a escolha. Sem registro, retorna enabled=false e radiusKm=5. |
| PUT /api/v1/client/notification-preferences | Corpo: {"enabled":true,"radiusKm":5}. Desativar também apaga a posição. |
| PUT /api/v1/client/notification-preferences/location | Corpo: {"latitude":-23.54,"longitude":-47.01,"accuracyMeters":20,"observedAt":"2026-11-02T12:00:00Z"}. Usar o instante atual real, não copiar a data deste exemplo. |
| DELETE /api/v1/client/notification-preferences/location | Apaga a posição, preservando a escolha de avisos. Retorna 204. |
| GET /api/v1/client/notification-preferences/stores | Até cem lojas mais próximas, com storeId, latitude, longitude e radiusMeters. A busca de preparação alcança duas vezes o raio; a geocerca de cada loja continua limitada ao raio escolhido de 5 ou 10 km. |

GET/PUT retornam enabled, radiusKm, locationObservedAt e locationExpiresAt; datas ausentes são null. A atualização de posição não habilita avisos automaticamente. Observações anteriores à já armazenada não substituem a mais recente.

Erros: 401 para token ausente/inválido; 404 para perfil não vinculado/ativo; 400 para raio, coordenadas, precisão ou data inválidos; 409 ao enviar posição sem adesão ativa. A precisão aceita fica entre 0 e 10.000 metros. As coordenadas e o endereço residencial não são devolvidos na resposta de preferência.

## Credencial de leitura do Redis

`POST /api/v1/client/notifications/access-token`, autenticado por Firebase, sem corpo, retorna:

```json
{"accessToken":"<JWT-curto>","expiresIn":300}
```

A resposta contém Cache-Control: no-store e Pragma: no-cache. O app usa o JWT somente em GET /v1/notifications e POST /v1/notifications/{id}/read na Redis API. Esse JWT não substitui Firebase no perfil, catálogo, pontos ou chat.

O destinatário numérico continua compatível com a troca de token existente:

```text
userId = 4294967296 + customer_id SQL
companyId = CLIENT_NOTIFICATIONS_COMPANY_ID
```

A empresa é um namespace estável de entrega do aplicativo Cliente, definido no servidor. Não é um mercado escolhido pelo telefone. A faixa de clientes fica separada dos IDs INTEGER de funcionários. Revisar esse contrato se outros produtores passarem a usar a mesma faixa ou os IDs de funcionários mudarem para BIGINT.

O JWT não contém CPF, e-mail, nome ou UID Firebase. A chave de assinatura fica somente nos servidores.

## Configuração e ordem de publicação

1. Aplicar a migração 42 no banco operacional de destino.
2. Publicar a API Redis com metadados de promoção e vencimento.
3. Publicar a API Cliente com o produtor inicialmente desabilitado.
4. Configurar as credenciais privadas e o namespace de entrega.
5. Habilitar o produtor e publicar o Android compatível com as rotas de preferência.

API Cliente:

```dotenv
CLIENT_NOTIFICATIONS_ENABLED=true
CLIENT_NOTIFICATIONS_JWT_SECRET=<chave Base64 privada com pelo menos 32 bytes>
CLIENT_NOTIFICATIONS_COMPANY_ID=<namespace positivo acordado>
CLIENT_NOTIFICATIONS_TTL_SECONDS=300
CUSTOMER_NOTIFICATION_DISPATCH_ENABLED=true
CUSTOMER_NOTIFICATION_REDIS_URL=https://<redis-api-privada>/
NOTIFICATION_PUBLISHER_SECRET=<segredo privado aleatório, 32 a 512 caracteres ASCII sem espaços>
CUSTOMER_NOTIFICATION_ALLOW_INSECURE=false
```

Na Redis API, configurar JWT_SECRET com a mesma chave Base64 da credencial de leitura, JWT_ALGORITHM=HS256 e NOTIFICATION_PUBLISHER_SECRET com o mesmo segredo privado do produtor. A chave de leitura e o segredo de publicação são credenciais diferentes. Não reutilizar a chave de funcionários e não colocar nenhuma delas no APK, SQL, documentação pública ou Git.

Para Docker local, a URL privada pode usar http com CUSTOMER_NOTIFICATION_ALLOW_INSECURE=true explícito. Produção usa HTTPS; o produtor não segue redirecionamentos. Sem configuração válida, a ativação falha na inicialização sem revelar os segredos. Quando desabilitado, o produtor não acessa a fila.

O produtor consulta a fila a cada 15 segundos, reserva até dez entregas por lote e libera a conexão SQL antes do HTTP. A chamada exige recibo 202 com id correspondente e status=queued. Configure persistência e monitoramento do Redis; 202 significa evento enfileirado, não confirmação de visualização no celular.

## Evento privado

POST /internal/v1/notifications na Redis API, com X-Mottainai-Notification-Secret, recebe:

```json
{"companyId":1,"userId":4294967308,"id":"customer-promotion-delivery-123","title":"Produto em oferta","body":"Confira esta oferta na loja.","priority":3,"promotionId":10,"storeId":20,"productId":30,"expiresAt":1793664000000}
```

Os valores são exemplos, sem representar uma pessoa real. expiresAt é um instante futuro em milissegundos UTC, limitado ao fim real da promoção. O Redis mantém createdAt em segundos UTC para compatibilidade com consumidores existentes. O Android deve converter segundos para milissegundos ao formatar a data.

IDs de destino são positivos. Os quatro campos de destino são enviados juntos. Avisos operacionais genéricos continuam funcionando sem eles. A API Cliente só confirma a publicação SQL após validar o recibo; em uma falha de confirmação, a repetição usa o mesmo ID e não reabre um aviso já lido.

## Validação

`mvn --batch-mode verify` executa os testes de contrato, limites, configuração e cadeia real de segurança HTTP. A validação Firebase externa e os serviços SQL são simulados nos testes web.

O teste NotificationPostgresIntegrationTest só é habilitado com NOTIFICATION_TEST_DB_URL exatamente igual a jdbc:postgresql://mottainai-notification-tests-db:5432/notification_test. Preparar antes a instalação do banco e a fixture 42_customer_promotion_notifications_fixture.sql. Ele usa o papel restrito, salva preferência/posição, consulta geocercas, publica em um HTTP local de teste e confirma ausência de nova reserva das entregas publicadas. Nunca apontá-lo ao banco oficial.

A homologação física continua necessária: localização em segundo plano, mudança de raio, duas contas, revogação de permissões, rede indisponível e abertura da promoção correta. Esta mudança não dispara campanhas de teste em contas reais automaticamente.