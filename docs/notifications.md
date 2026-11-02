# Avisos do Mobile Cliente

## O que esta implementação entrega

Troca de Firebase ID token por um JWT HS256 curto, aceito pelo contrato atual da
[API Redis](https://github.com/Mottainai-One/Mottainai-Redis-Api/tree/82d9592).
Não há criação de tabelas, funções SQL, políticas RLS ou permissões novas.
O perfil ativo é resolvido pelo fluxo Firebase/SQL existente, com as permissões atuais.

```http
POST /api/v1/client/notifications/access-token
Authorization: Bearer <Firebase-ID-token>
```

Sem corpo. Resposta `200`, com `Cache-Control: no-store` e `Pragma: no-cache`:

```json
{"accessToken":"<JWT-curto>","expiresIn":300}
```

O app usa o resultado exclusivamente para `GET /v1/notifications?limit=100` e
`POST /v1/notifications/{id}/read` na API Redis. O token não substitui o Firebase
nas rotas de perfil, catálogo ou pontos, nem deve ser usado no chat.

- `401`: Firebase ID token ausente ou inválido, inclusive token legado ou de notificações.
- `404`: perfil ausente/inativo no fluxo SQL existente.
- `403`: perfil resolvido sem identidade positiva ou sem estado ativo.
- `503`: integração desabilitada. Os demais recursos continuam funcionando.

## Configuração nos servidores

Na **API SQL Cliente**:

```dotenv
CLIENT_NOTIFICATIONS_ENABLED=true
CLIENT_NOTIFICATIONS_JWT_SECRET=<chave Base64 privada com ao menos 32 bytes>
CLIENT_NOTIFICATIONS_COMPANY_ID=<namespace positivo de entrega definido pela equipe>
CLIENT_NOTIFICATIONS_TTL_SECONDS=300
```

Na instância da **API Redis destinada ao Cliente**, `JWT_SECRET` deve conter a mesma
chave Base64 e `JWT_ALGORITHM=HS256`. Preferir uma chave exclusiva dessa integração,
separada dos tokens de funcionários e do login legado. Nunca configurar a chave no APK,
em documentação pública, no SQL de carga ou no Git. O segredo não tem valor padrão
nem fallback para a chave legada. O tempo de vida aceito é de 30 a 300 segundos.
Uma configuração inválida quando habilitada impede a inicialização, sem revelar a chave.

`CLIENT_NOTIFICATIONS_COMPANY_ID` representa o namespace de entrega comum do aplicativo
Cliente. Não é o mercado mais próximo nem uma empresa escolhida pelo telefone. O servidor
e o produtor devem combinar um valor estável: clientes podem receber ofertas de várias
lojas em uma única caixa. Alterá-lo muda a caixa consultada e deixa avisos anteriores
no namespace antigo até expirarem. Esta rota não faz migração de caixas existentes.

O JWT contém `sub`, `usuario_id`, `empresa_id`, `role=CLIENTE`, `use=access`, `iat`,
`exp`, `jti`, emissor `mottainai-client-notifications` e audiência
`mottainai-redis-notifications`. Não inclui CPF, e-mail, nome ou UID Firebase.
O AuthService Redis atual valida assinatura, validade e identidade; ainda não exige
emissor/audiência. Esses campos não substituem a separação de chave entre serviços.

## Isolamento e destinatário do evento

A API Redis atual separa caixas por empresa e usuário numérico, sem considerar `role`.
Copiar diretamente `customer_id=12` poderia abrir a caixa de `employee_id=12`.
O SQL atual usa `INTEGER` positivo para ambos; por isso o contrato reserva:

```text
user_id do destinatário Cliente = 4.294.967.296 + customer_id
company_id do evento = CLIENT_NOTIFICATIONS_COMPANY_ID
```

Exemplo de contrato, sem representar uma pessoa real: `customer_id=12` corresponde
a `user_id=4294967308`. A faixa Cliente vai de `4294967297` a `6442450943`, fora da
faixa positiva de `INTEGER` dos funcionários. O produtor precisa fazer a mesma conta
com inteiro de 64 bits. Nenhum desses valores é aceito do aparelho na troca de token.

Se outros produtores já usarem IDs nessa faixa, ou se IDs de funcionários passarem
a `BIGINT`, revisar este contrato antes de habilitar a integração. Não reaproveitar
esse identificador de entrega como chave SQL em vendas, perfil, IA ou fidelidade.

## Como o disparo deve funcionar

1. Um produtor autorizado identifica uma promoção aprovada, ativa, vigente e disponível.
2. O servidor seleciona os clientes elegíveis. A regra de negócio deve considerar
   lojas parceiras, posição consentida/atualizada e raio de 5 ou 10 km, no máximo
   três produtos distintos por loja por dia e prioridade temporal por fidelidade.
3. Para cada destinatário, o produtor publica um evento no Redis Stream
   `mottainai:v1:notification:stream` (ou na chave configurada em `NOTIFICATION_STREAM_KEY`).
4. `NotificationStreamListener`, na API Redis, valida o evento e grava a caixa com
   `NotificationWriter`. Esse componente atribui `createdAt` em milissegundos UTC.
5. O Android obtém a credencial curta na API Cliente e consulta sua caixa na Redis API.
   Ao encontrar IDs inéditos, mostra uma notificação nativa e evita repetir o mesmo aviso.
6. Ao consultar um aviso, o usuário pode abrir as ofertas ou marcá-lo como lido.

Formato do evento de transporte, **não uma rota HTTP pública**:

```json
{
  "company_id": "<CLIENT_NOTIFICATIONS_COMPANY_ID>",
  "user_id": "<4294967296 + customer_id>",
  "id": "promotion-123-customer-12-20261102",
  "title": "Uma oferta para você",
  "body": "Confira as ofertas disponíveis no aplicativo.",
  "priority": "2"
}
```

Usar um ID estável por aviso/destinatário nas tentativas, com caracteres alfanuméricos,
ponto, hífen ou sublinhado e até 128 caracteres. `title` admite até 120 caracteres,
`body` até 1000 e `priority` deve estar entre 1 e 3. A prioridade da caixa Redis não
implementa, por si, a janela de envio antecipado por fidelidade.

O produtor deve usar uma conexão Redis privada com ACL apropriada. O celular não publica
eventos e não pode escolher o destinatário. Esta implementação não cria uma rota de disparo
pública e não envia campanhas para pessoas reais durante os testes.

## O que continua pendente fora desta troca de token

Na IA `main` examinada em `c416945`, `app/cache/notifications.py` contém a função de escrita,
mas não foi encontrado um chamador de publicação automática em `app`/`config`.
Ela grava `created_at` em segundos; não reutilizar essa escrita direta para o mobile sem
padronizar as unidades. Preferir o Stream e o Writer da Redis API, que usam milissegundos.
É necessário implementar e homologar o produtor de promoções e a seleção dos destinatários.
Esta rota de credencial não implementa localização em segundo plano, frequência diária,
prioridade por pontos ou disparo automático ao cadastrar uma promoção.

O contrato Redis atual não retorna `promotionId` e `storeId`. Acrescentar esses campos
ao produtor, armazenamento, resposta e Android para abrir uma promoção específica.
No contrato atual, o app abre o catálogo geral, sem deduzir IDs a partir do texto.

## Android e frequência de entrega

Depois de publicar a rota e configurar os dois servidores, definir no ambiente privado Android:

```properties
MOTTAINAI_NOTIFICATIONS_BASE_URL=https://<servidor-redis>/
MOTTAINAI_NOTIFICATIONS_TOKEN_PATH=api/v1/client/notifications/access-token
```

Recompilar o APK. Para testes locais por USB, encaminhar também a porta da API Redis
com `adb reverse tcp:8083 tcp:8083`, mantendo o encaminhamento da API Cliente.
Usar HTTP somente no build de desenvolvimento local; produção requer HTTPS.
O cliente deve ativar avisos e conceder a permissão de notificações no Android.

A integração atual consulta a caixa aproximadamente a cada minuto em primeiro plano.
Em segundo plano, usa JobScheduler com intervalo mínimo de 15 minutos, sujeito a adiamentos
do Android. Não é push instantâneo; força de parada, Doze e bateria podem suspender/adiar
as consultas. A proximidade instantânea exige outra etapa de arquitetura. Não foi adicionado
Firebase Messaging ou Firestore; Firebase continua dedicado à autenticação do Cliente.

## Validação

Executar `mvn test`. Os testes isolados verificam assinatura HS256, campos e duração,
IDs de contas diferentes, falhas de perfil/configuração, ausência de cache e cadeia HTTP.
Somente a validação Firebase externa e a consulta de perfil SQL são simuladas no teste web.
Isso não substitui homologar um Firebase ID token real e as permissões SQL no ambiente.

Após configurar, validar em ambiente de homologação: duas contas com caixas distintas;
evento publicado com o destinatário mapeado; listagem e leitura idempotente na API Redis;
notificação nativa; ausência de repetição; logout; permissão negada; token expirado; falha
de rede sem encerrar a sessão principal. Não fazer uma campanha real para testar a rota.
