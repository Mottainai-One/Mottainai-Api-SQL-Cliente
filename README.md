# Mottainai API SQL Cliente

## CI

O workflow [ci.yml](.github/workflows/ci.yml) executa em pull requests, inclusive
edição da descrição, push em `main` e execução manual.

Os cinco critérios solicitados aparecem como checks individuais e exigem os itens
marcados no [checklist da PR](.github/pull_request_template.md). São declarações de
revisão: não comprovam, por si só, compilação, testes, inicialização ou instalação.
Comentários HTML e exemplos em blocos de código não contam como itens marcados.
Para itens sem impacto, registre uma justificativa na descrição antes de marcá-los.

| Critério | Evidência esperada |
| --- | --- |
| O projeto compila sem erros. | Comando de build e resultado |
| Os testes automatizados passam. | Comando de testes, quantidade executada e resultado |
| A aplicação inicia sem erros. | Comando de inicialização e verificação de prontidão |
| O arquivo .env e outras informações sensíveis não foram enviados ao repositório. | Jobs de Gitleaks e arquivos de ambiente aprovados |
| As dependências necessárias estão configuradas e podem ser instaladas. | Manifesto, lockfile quando aplicável e instalação em ambiente limpo |

### Verificações automáticas

- Gitleaks varre o histórico Git disponível com valores sensíveis ocultos nos logs.
  A imagem está fixada por digest e não precisa de licença ou secrets adicionais.
- O validador de arquivos rejeita `.env`, `.env.*`, `*.env` e `*.env.*` versionados,
  inclusive em subpastas. Apenas `.env.example`, `.env.sample` e `.env.template`
  são permitidos como modelos; o Gitleaks também os examina.
- Os validadores do CI possuem testes automatizados, executados em Python 3.13.
  Python é usado para o CI e não define a linguagem da futura aplicação.
- As actions estão fixadas por SHA, com permissões de leitura, checkout sem
  credenciais persistentes e limites de duração.

Nenhuma varredura garante detectar todos os segredos. Se uma credencial real for
encontrada, revogue-a; removê-la apenas da versão atual não limpa o histórico.

### Estado da aplicação

Na criação deste CI, o repositório continha somente `LICENSE`. Ainda não existem
código de aplicação, manifesto de dependências, build, testes funcionais ou comando
de inicialização. Esses passos deverão ser adicionados ao workflow quando a stack
for definida. O job de infraestrutura não deve ser interpretado como teste da aplicação.

### Validação local

```shell
python -m unittest discover -s tests/ci -v
python scripts/ci/check_env.py
```

### Proteção de merge

Para impedir merges com checks pendentes ou falhando, um administrador precisa
exigir os cinco critérios e os três jobs automáticos nas regras de `main`.
Esta PR não modifica as regras de proteção.
