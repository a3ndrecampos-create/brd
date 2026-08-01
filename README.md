# Tá Pago 🏃‍♂️📸

App Android de corrida/caminhada que incentiva cuidar da saúde: rastreia o
percurso via GPS e, ao final, tira uma foto que já sai com as estatísticas
da atividade (distância, tempo, ritmo, calorias) sobrepostas — pronta para
compartilhar no Instagram Stories em um toque.

Inspirado no "Stats Sticker" do Strava, mas com o overlay renderizado
**nativamente pelo próprio app** (não depende do editor do Instagram).

## Stack

Kotlin + Jetpack Compose, Clean Architecture (domain/data/presentation),
modularização por feature, Hilt para DI, CameraX para captura de foto,
FusedLocationProviderClient para GPS, kotlinx.serialization para rotas
tipadas de navegação.

## Estrutura

```
app/                    # módulo de aplicação — navegação e Application class
core/
  designsystem/         # tema, cores, tipografia (Material 3)
  common/                # Outcome<T> e utilitários compartilhados
  database/              # placeholder — Room será adicionado quando o
                          # histórico de corridas for implementado
  network/                # placeholder — endpoints de backend futuros
feature/
  tracking/               # rastreamento por GPS + tela de corrida/caminhada
  photoshare/              # câmera + overlay de estatísticas + share pro Instagram
```

## Fluxo principal implementado

1. Usuário escolhe "Iniciar corrida" ou "Iniciar caminhada" (`TrackingScreen`)
2. O app rastreia a rota via GPS em tempo real, mostrando distância/tempo/ritmo
3. Ao finalizar, a sessão é salva e o app navega para `PhotoShareScreen`
4. Usuário tira uma foto (CameraX); o app desenha o overlay de estatísticas
   sobre o bitmap (`StatsOverlayComposer`)
5. Um toque em "Compartilhar no Instagram Stories" abre o Instagram já com
   a imagem pronta como story (`InstagramStoriesSharer`), com fallback para
   o share sheet padrão do Android se o Instagram não estiver instalado

## Como abrir

1. Abra a pasta no Android Studio (Koala ou mais recente)
2. Sincronize o Gradle
3. Rode o módulo `app` num dispositivo/emulador com câmera e GPS

> Builds de release **não** são feitos localmente — apenas via GitHub
> Actions (`.github/workflows/release.yml`), usando a keystore como secret
> em Base64. Nenhuma credencial fica no repositório.

## CI

- `.github/workflows/ci.yml`: roda em cada PR — ktlint, detekt, testes
  unitários + cobertura (Kover), build de debug
- `.github/workflows/release.yml`: disparado por tag `v*` — build assinado
  do App Bundle

## Próximos passos (fora do escopo deste incremento)

- [ ] Persistência com Room (hoje as sessões ficam em memória, perdidas ao
      fechar o app) — trocar `InMemoryRunSessionRepository`
- [ ] Tela de histórico de corridas
- [ ] Mapa real do percurso (Google Maps Compose) na tela de tracking e no
      overlay da foto
- [ ] Onboarding + permissões (localização em segundo plano, câmera)
- [ ] Monetização: anúncios + assinatura com teste grátis (Play Billing),
      conforme especificação técnica original
- [ ] Testes de UI (Compose Testing) para `TrackingScreen` e `PhotoShareScreen`
- [ ] Ícone do app e splash screen
