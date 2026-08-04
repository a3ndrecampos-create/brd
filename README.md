# BeautyManager

Sistema de gestão para loja de cosméticos (PDV + estoque + CRM + lembretes de
recompra), pensado para rodar 100% local em um único aparelho — sem backend, sem
conta na nuvem, sem mensalidade de servidor. Arquitetura preparada para, no
futuro, suportar múltiplas lojas/dispositivos trocando os repositórios locais por
uma implementação com sincronização, sem tocar em domínio ou telas.

## Stack

- Kotlin + Jetpack Compose (Material 3) — identidade visual própria (mauve/dourado),
  sem Material You dinâmico, para manter a marca consistente em qualquer aparelho
- Arquitetura em camadas: `domain` (modelos, contratos, regras de negócio) →
  `data` (Room + Retrofit) → `presentation` (MVVM: ViewModel + Compose)
- Hilt para injeção de dependência
- Room (SQLite) para persistência local
- DataStore para sessão (usuário logado, biometria, tema)
- Retrofit + kotlinx.serialization para consultar a **Bluesoft Cosmos**
  (`https://cosmos.bluesoft.com.br`) — base de produtos brasileira, tentada
  primeiro (exige token cadastrado), com fallback automático para a **Open
  Beauty Facts** (pública, sem chave) — usadas só para sugerir nome/marca/foto
  ao ler um código de barras novo
- CameraX + ML Kit Barcode Scanning para o leitor de código de barras (câmera)
- WorkManager (+ Hilt Worker) para recalcular os lembretes de recompra uma vez por dia
- Gráficos feitos com Canvas nativo do Compose (sem lib de terceiros)

## Estrutura

```
domain/
  model/        Product, Customer, Sale, SaleItem, StockMovement, Reminder,
                ReminderRule, AppUser, DashboardMetrics, CartItem...
  repository/    Contratos (interfaces) — o único ponto que o domínio conhece
  usecase/       RegisterSaleUseCase, LookupProductByBarcodeUseCase,
                GenerateRemindersUseCase, DashboardMetricsUseCase,
                BuildWhatsAppMessageUseCase

data/
  local/entity/  Entidades Room (1:1 com as tabelas)
  local/dao/     Queries, incluindo as agregações (faturamento, lucro, top
                produtos, aniversariantes, última compra por categoria)
  local/database/ BeautyManagerDatabase (Room)
  remote/barcode/ BarcodeApi (Open Beauty Facts)
  repository/    Implementações + mappers Entity <-> Domínio

presentation/
  auth/          Login por PIN compartilhado (papéis Admin/Funcionário)
  dashboard/     Métricas do dia + gráfico de produtos mais vendidos
  products/      Lista, formulário (com scanner de código de barras)
  sales/         PDV (carrinho, desconto, forma de pagamento, checkout)
  customers/     Lista + perfil completo (histórico, ticket médio, etc.)
  stock/         Entrada/saída/ajuste/transferência + histórico
  reports/       Faturamento/lucro por período + produtos mais vendidos
  reminders/     Lista de lembretes pendentes + envio via WhatsApp
  settings/      Categorias, marcas, fornecedores, regras de lembrete, usuários, tema
  navigation/    Gate de login + shell com bottom nav

core/
  di/            Módulos Hilt (Database, Network, Repository)
  util/          SecurityUtils (hash de PIN)
  work/          DailyMaintenanceWorker (regenera lembretes 1x/dia)
```

## Decisões importantes (e por quê)

- **100% local por enquanto**: nenhuma sincronização entre dispositivos. Se no
  futuro for necessário multi-loja/multi-funcionário em dispositivos diferentes,
  o ponto de troca é `RepositoryModule` — os `UseCase`s e telas não mudam.
- **Bluesoft Cosmos com fallback para Open Beauty Facts**: a Cosmos tem cobertura
  muito melhor de produtos brasileiros de mercado/farmácia/cosméticos, mas exige
  cadastro (token + User-Agent). Sem essas credenciais configuradas, o app cai
  automaticamente para a Open Beauty Facts (pública, cobertura menor mas sem
  fricção nenhuma) — nunca trava o cadastro por falta de token.
- **PIN com hash SHA-256 + salt fixo**: funcional para este estágio, mas antes de
  ir para produção o ideal é trocar o salt fixo por um salt por instalação gerado
  no Android Keystore.
- **Sem lib de gráficos de terceiros**: o Vico (uma opção comum) ainda está em
  beta e sua API muda entre versões; para os gráficos simples que o app precisa
  (barras de produtos mais vendidos), um Canvas nativo é mais previsível e leve.

## Configurando a Bluesoft Cosmos (opcional, mas recomendado)

1. Cadastre-se em https://cosmos.bluesoft.com.br e pegue seu **token** e
   **User-Agent** (ambos ficam disponíveis na sua área logada).
2. Copie `local.properties.example` para `local.properties` (esse arquivo já
   está no `.gitignore` — nunca vai para o Git) e preencha:
   ```
   COSMOS_API_TOKEN=seu_token_aqui
   COSMOS_USER_AGENT=seu_user_agent_aqui
   ```
3. Rodando localmente pelo Android Studio, isso já é suficiente.
4. Para o **GitHub Actions** também usar a Cosmos no APK gerado, configure os
   mesmos dois valores em Settings → Secrets and variables → Actions →
   New repository secret (`COSMOS_API_TOKEN` e `COSMOS_USER_AGENT`).

Sem nada disso configurado, o app funciona normalmente usando só a Open Beauty
Facts.

## CI/CD

Existe um workflow em `.github/workflows/build-apk.yml` que compila um APK de
debug a cada push/PR nas branches `main`/`master` (ou manualmente pela aba
Actions, botão "Run workflow"). O APK fica disponível como artefato da execução
— aba **Actions** do repositório → clique na execução → seção **Artifacts**
(`beautymanager-debug-apk.zip`, contém o `.apk` dentro). Não é um APK assinado
para a Play Store, é o build de debug para testar no aparelho.

## O que já foi fechado nesta rodada

1. **Biometria de verdade**: `BiometricPrompt` funcional (login sem PIN quando
   habilitado em Configurações, por usuário).
2. **Marca casada automaticamente**: ao ler um código de barras novo, o nome de
   marca vindo da base consultada agora vira (ou casa com) um `Brand` real no
   catálogo, em vez de ficar como texto solto.
3. **Permissões por funcionário**: `AppUser.canManageProducts/canViewReports/
   canManageUsers` agora realmente escondem, respectivamente, o botão de
   adicionar/editar produto, o item "Relatórios" do menu e a seção "Usuários"
   em Configurações.
4. **Comprovante de venda**: ao finalizar uma venda no PDV, aparece um resumo
   com itens/desconto/total e um botão "Compartilhar" (abre o seletor do
   Android — WhatsApp, e-mail, etc. — com o comprovante em texto).
5. **Backup/restauração**: em Configurações, "Exportar backup" salva um
   `.json` com todo o banco (produtos, clientes, vendas, estoque...) via seletor
   de arquivos do Android; "Restaurar backup" lê um `.json` e substitui os dados
   atuais (com confirmação explícita, porque é destrutivo).
6. **Bluesoft Cosmos** como primeira fonte de busca por código de barras
   (fallback automático para Open Beauty Facts).
7. **Foto do produto manual**: além da foto vinda da busca automática, dá pra
   escolher uma foto da galeria no cadastro do produto.
8. **Scanner de código de barras no PDV**: além de buscar por nome, agora dá
   pra ler o código de barras direto na tela de venda e o produto cai no
   carrinho automaticamente.
9. **Data de aniversário do cliente**: cadastro e edição agora pedem a data de
   nascimento (com seletor de calendário) — isso alimenta de verdade o card
   "Aniversariantes do mês" do Dashboard, que antes nunca tinha dado.
10. **Cliente obrigatório na venda**: o PDV agora pede pra selecionar (ou
    buscar) um cliente antes de finalizar, com uma opção explícita "Venda sem
    identificar cliente" para vendas de balcão rápidas — a venda nunca mais
    fica "sem ninguém" atrelada sem essa decisão consciente.
11. **`.gitignore` do projeto**: não existia; sem ele, o `local.properties`
    (com o token da Cosmos) poderia acabar indo pro Git por engano.

## O que ainda é TODO (próximas rodadas)

1. **Exportação de relatórios em PDF/Excel** — os botões já estão na tela de
   Relatórios, falta ligar a geração do arquivo (ver skill de PDF do projeto para
   PDF; para Excel, evitar Apache POI em mobile — considerar uma lib leve de xlsx
   ou até CSV como primeira versão).
2. **Impressão térmica do comprovante** — hoje o comprovante é compartilhado como
   texto; impressão de verdade depende do SDK do modelo de impressora térmica
   usado na loja (ex.: Epson ePOS, Bematech).
3. **Sugestões com IA** (mencionado no briefing): próxima probabilidade de recompra
   por cliente, recomendação de produto por cliente e previsão de que mensagens de
   WhatsApp convertem mais — é um módulo à parte, precisa de dados acumulados de
   uso antes de valer a pena implementar.

## Rodando o projeto

Abra a pasta no Android Studio (Ladybug ou mais recente), deixe o Gradle
sincronizar e rode no emulador ou aparelho físico (`minSdk 26`). Funciona sem
nenhuma chave de API (usa a Open Beauty Facts, pública) — para usar a Bluesoft
Cosmos também, veja a seção "Configurando a Bluesoft Cosmos" acima.
Ou, sem instalar nada, deixe o GitHub Actions gerar o APK (ver seção CI/CD acima).
