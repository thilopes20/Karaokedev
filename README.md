# KaraokeDev

App de karaokê próprio para estudo — grava sua voz, aplica efeitos em tempo real e mostra a letra sincronizada. 100% código próprio, sem conteúdo de terceiros embutido: você importa suas músicas e letras `.lrc` do aparelho.

## O que já funciona

- Captura do microfone e playback da voz processada em tempo real (44,1 kHz mono)
- Cadeia de efeitos com blocos DSP clássicos: noise gate → EQ (highpass + presença) → compressor → pitch shift → reverb (Freeverb) → echo
- 4 presets: **Studio**, **Echo**, **Radio**, **Chipmunk**
- Letras `.lrc` sincronizadas com a música (formato aberto, amplamente suportado)
- Exportação da voz processada em WAV (pasta `gravacoes` do app)
- Módulo de biblioteca pronto para plugar Supabase (`SupabaseMusicSource`)

## Requisitos

- Android Studio (recente) com SDK 35 e JDK 17
- Um celular Android 8.0+ para testar (áudio real importa: microfone e latência)

## Como rodar

1. Abra a pasta no Android Studio (File → Open) e aguarde a sincronização do Gradle
2. Se o Android Studio perguntar sobre o Gradle wrapper, aceite gerar/usar o wrapper padrão (ou use a distribuição local do próprio Studio)
3. Conecte o celular via USB com Depuração USB ativada
4. Rode o app (▶) no celular
5. Conceda a permissão de microfone
6. Escolha um MP3 do aparelho, escolha uma letra `.lrc`, selecione um preset e toque em **Cantar**

Para gerar letras `.lrc`: existem bancos públicos de letras sincronizadas, ou você pode criar as suas — o formato é simples:

```
[00:12.50] Primeira linha da música
[00:16.80] Segunda linha
```

## Como afinar os efeitos

Os presets ficam em `app/src/main/java/com/karaokedev/app/audio/Presets.kt`. Cada preset é uma receita de parâmetros sobre os mesmos blocos:

| Parâmetro | O que faz | Dica |
|---|---|---|
| `gateThreshold` | Ruído cortado abaixo deste nível | 0,005–0,03 (menor = menos corte) |
| `highpassHz` | Corta graves da voz | 80–120 Hz natural; 300 Hz para "rádio" |
| `presenceGainDb` | Realça clareza em ~3 kHz | +2 a +6 dB |
| `compressThreshold` | Uniformiza volume | 0,2–0,4 |
| `reverbDecay` | Tamanho da "sala" | 0,6–0,8; acima de 0,85 pode "lavrar" o som |
| `reverbMix` | Quantidade de reverb | 0,15–0,30 |
| `echoTimeMs` / `echoFeedback` | Eco e repetições | 150–400 ms; feedback até 0,5 |
| `pitchRatio` | Tom da voz | 1,3–1,5 chipmunk; correção de pitch fica no roadmap |

Fluxo de afinação: mude um parâmetro → rode no celular → cante um trecho → compare A/B com o som de referência → repita.

## Plugando o Supabase

O app já traz `SupabaseMusicSource` no pacote `library`. No seu projeto Supabase:

```sql
create table karaoke_songs (
  id uuid primary key default gen_random_uuid(),
  title text not null,
  audio_url text not null,
  lyrics_url text
);
alter table karaoke_songs enable row level security;
create policy "leitura publica" on karaoke_songs for select using (true);
```

Crie um bucket público `karaoke-audios`, suba os áudios e cadastre as linhas com as URLs públicas. No app, instancie:

```kotlin
val source = SupabaseMusicSource("https://SEU-PROJETO.supabase.co", "SUA_ANON_KEY")
val songs = source.listSongs()
```

Importante: cadastre apenas músicas que você tem o direito de usar (próprias, livres ou domínio público).

## Roadmap de estudos (na ordem)

1. **Afinar os presets** até o som agradar — é o treino de ouvido
2. **Correção de pitch** (o "autotune"): detectar a nota (algoritmo YIN) e puxar para a nota alvo
3. **Migração para C++ via NDK + Oboe**: mesma cadeia, latência profissional
4. **Bibliotecas nativas**: Rubber Band (pitch/time-stretch de qualidade) e Freeverb em C++
5. **Mixagem com a base**: gravar voz e instrumental em trilhas separadas e mixar com ganhos independentes
6. **Cache local + Supabase**: baixar músicas para uso offline

## Limitações conhecidas desta versão

- A voz é reproduzida processada em tempo real, mas o áudio da base toca por caminho separado (MediaPlayer) — a gravação WAV salva contém apenas a voz processada
- O pitch shift é por reamostragem simples (muda velocidade e tom juntos) — é o bloco que primeiro merece migração para C++ (PSOLA/Rubber Band)
- Latência maior que apps nativos em C++ — esperado nesta fase Kotlin, resolvido no roadmap item 3

## Licenças

Todo o código deste repositório é seu. As técnicas de DSP usadas (filtros biquad RBJ, reverb estilo Freeverb/Schroeder) são domínio público e bem documentadas.
