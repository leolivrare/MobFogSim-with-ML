# MobFogSim Migration API

Esta API expõe um modelo treinado para tomar decisões de migração de dispositivos móveis. O objetivo é, dado um conjunto de parâmetros referentes à posição, direção, velocidade e distâncias do dispositivo em relação a pontos de interesse (como pontos de acesso e cloudlets), retornar se o dispositivo deve ou não migrar (true/false).

**Adendo:** O modelo executado pela API é apenas um exemplo simples, criado para provar a viabilidade de integrar e acionar a API a partir do simulador MobFogSim. Ainda assim, a arquitetura da API permite o carregamento de modelos mais complexos e avançados, bastando substituir o arquivo de checkpoint e ajustar os parâmetros no código.

## Funcionamento da API

A API é construída usando FastAPI e PyTorch. Ao iniciar, a API carrega um modelo previamente treinado a partir do arquivo `decision_model.pth`. Esse modelo já foi ajustado anteriormente para receber duas features (`IsMigPoint` e `IsMigZone`) e, com base nisso, retornar a probabilidade do dispositivo precisar migrar.

- O modelo é carregado na memória assim que a API inicia.
- A rota POST `/should_migrate` recebe um JSON com diversos parâmetros do dispositivo, mas internamente a inferência só utiliza `IsMigPoint` e `IsMigZone`.
- O modelo retorna uma pontuação (logit), que é convertida em probabilidade. Se a probabilidade for maior que 0.5, o resultado será `shouldMigrate = true`, caso contrário `false`.

## Executando a API

Existem duas formas principais de executar a API: localmente com virtualenv (venv) e via Docker.

### Opção 1: Virtualenv (local)

1. Crie e ative o ambiente virtual:
   ```bash
   python3 -m venv venv
   source venv/bin/activate
   ```

2. Instale as dependências:
   ```bash
   pip install -r requirements.txt
   pip install torch==2.5.1 --extra-index-url https://download.pytorch.org/whl/cpu
   ```

3. Execute a API:
   ```bash
   uvicorn api:app --host 0.0.0.0 --port 8000
   ```

A aplicação estará disponível em `http://127.0.0.1:8000/should_migrate`.

### Opção 2: Docker

1. Construa a imagem Docker:
   ```bash
   docker build -t ml_api:latest .
   ```

2. Rode o container:
   ```bash
   docker run -p 8000:8000 -v $(pwd)/logs.txt:/app/logs.txt ml_api:latest
   ```

A aplicação estará disponível em `http://127.0.0.1:8000/should_migrate`.

## Logs

A API gera logs detalhados de seu funcionamento, incluindo:
- Carregamento do modelo.
- Requisições recebidas no endpoint.
- Decisões tomadas pelo modelo.

Esses logs são salvos no arquivo `logs.txt` presente na raiz do projeto. Dessa forma, é possível monitorar e auditar o comportamento da API e do modelo.

**Exemplos de logs**:
```
2024-12-08 15:15:26,151 INFO: Model loaded successfully
2024-12-08 15:19:31,272 INFO: Received request: {"PosX":7473.0,"PosY":4967.0,"Direction":5.0,"Speed":0.0,"DistanceToSourceAp":646.623538080698,"DistanceToLocalCloudlet":646.623538080698,"DistanceToClosestCloudlet":883.7250703697389,"IsMigPoint":false,"IsMigZone":true}
2024-12-08 15:19:31,273 INFO: Decision: False for request: PosX=7473.0, PosY=4967.0, Direction=5.0, Speed=0.0
2024-12-08 15:19:31,354 INFO: Received request: {"PosX":7471.0,"PosY":4967.0,"Direction":5.0,"Speed":2.0,"DistanceToSourceAp":648.2669203345177,"DistanceToLocalCloudlet":648.2669203345177,"DistanceToClosestCloudlet":881.9081584836371,"IsMigPoint":false,"IsMigZone":true}
```