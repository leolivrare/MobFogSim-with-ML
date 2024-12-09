# PFG-UNICAMP: Integração de Machine Learning com o Simulador MobFogSim

## Visão Geral

Este projeto tem como objetivo possibilitar a integração de modelos de Machine Learning (ML) ao simulador **MobFogSim**, permitindo, por exemplo, suportar processos de tomada de decisão, como decisões de migração em ambientes de computação em névoa. Além disso, o projeto também investiga o funcionamento do treinamento federado, aproveitando o cenário do simulador, que é baseado em dispositivos móveis (IoT). Esse contexto cria um ambiente ideal para experimentos de aprendizado federado colaborativo entre dispositivos simulados. O estudo sobre treinamento federado utiliza o framework **Flower**, uma ferramenta robusta e adequada para alcançar os objetivos deste projeto.

O projeto é composto por três principais componentes:

1. **Simulador MobFogSim**: Um fork do [MobFogSim original](https://github.com/diogomg/MobFogSim), aprimorado com suporte para tomadas de decisão baseadas em ML.
2. **Módulo de Aprendizado Federado**: Um framework para treinar modelos de ML utilizando logs gerados por dispositivos simulados em um cenário de aprendizado federado.
3. **API de Integração de ML**: Uma API para carregar modelos de ML treinados e integrá-los no processo de tomada de decisão do MobFogSim.

Embora o modelo de ML utilizado no projeto seja deliberadamente simples para facilitar experimentos, o sistema está totalmente preparado para suportar modelos mais complexos.

---

## Módulos do Projeto

### 1. Simulador MobFogSim

**MobFogSim** é um simulador poderoso para modelar mobilidade e migração em computação em névoa. Desenvolvido como uma extensão do framework **iFogSim**, o MobFogSim incorpora funcionalidades de mobilidade, permitindo que pesquisadores simulem cenários envolvendo dispositivos móveis e migração de recursos entre nós de névoa.

- **Detalhes do Fork**: Este projeto é um fork do [repositório MobFogSim](https://github.com/diogomg/MobFogSim). O código do simulador na pasta `MobFogSim/` permanece amplamente inalterado em relação ao projeto original. Todo o crédito vai para os autores do simulador e seu trabalho detalhado em viabilizar a pesquisa em computação em névoa e borda.
  
- **Melhorias**:
  1. **Melhoria nos Logs**: Logs detalhados por dispositivo foram criados para dar suporte ao aprendizado federado. Esses logs fornecem dados granulares sobre cada dispositivo, incluindo decisões de mobilidade e migração.
  2. **Tomada de Decisão Baseada em ML**: A lógica de tomada de decisão de migração foi estendida para incluir interações com um modelo de ML externo por meio da **API de Integração de ML**.

Para um entendimento mais profundo do MobFogSim, consulte o [artigo original](https://www.sciencedirect.com/science/article/abs/pii/S1569190X19301935?via%3Dihub).

Um guia detalhado para executar e usar o simulador está disponível em `MobFogSim/README.md`.

---

### 2. Módulo de Aprendizado Federado

Este módulo oferece um framework para o treinamento federado de modelos de ML utilizando dados de dispositivos simulados.

- **Logs como Dados**: Os logs gerados pelo MobFogSim servem como dados de entrada para o treinamento. Os logs de cada dispositivo correspondem a um cliente federado.
- **Aprendizado Federado com Flower**: O [framework Flower](https://flower.dev) viabiliza o treinamento descentralizado, permitindo que os clientes compartilhem apenas os parâmetros do modelo, mantendo os dados brutos privados.
- **Notebook**: Todos os detalhes de implementação e explicações estão documentados no notebook `ml_simple_model_training/PFG_modelo_federado.ipynb`. Ele guia os usuários por:
  - Pré-processamento de dados.
  - Fluxo de trabalho de treinamento federado.
  - Salvamento de modelos treinados para integração com a API.

> **Nota**: Embora o modelo de ML usado no notebook seja simples, o design modular permite a integração de modelos mais sofisticados.

---

### 3. API de Integração de ML

A **API de Integração de ML** conecta os modelos de ML treinados ao simulador MobFogSim, permitindo a tomada de decisões dinâmicas baseadas em ML durante as execuções da simulação.

- **Funcionalidades**:
  - Carrega um modelo de ML treinado (por exemplo, o gerado no módulo de aprendizado federado).
  - Fornece previsões por meio de uma API RESTful HTTP.
  - Integra-se perfeitamente ao MobFogSim para decisões de migração.

- **Componentes**:
  - `api.py`: Lógica central da API, utilizando FastAPI.
  - `Dockerfile`: Permite o deploy containerizado da API.
  - `logs.txt`: Captura logs detalhados de requisições e respostas da API para depuração e análise.

Para instruções de configuração e uso, consulte o `mobfogsim_migration_api/README.md`.

---

## Como os Módulos Interagem

1. **Simulador Gera Logs**: O MobFogSim gera logs detalhados da atividade de cada dispositivo, incluindo dados sobre mobilidade, proximidade e decisões de migração.
2. **Aprendizado Federado**: Os logs são utilizados para treinar modelos de ML via aprendizado federado no notebook.
3. **Integração do Modelo**: Modelos de ML treinados são carregados na API, que interage com o MobFogSim para influenciar as decisões de migração.

Essa arquitetura modular garante flexibilidade, permitindo que pesquisadores:
- Substituam o modelo de ML por alternativas mais complexas.
- Modifiquem o processo de treinamento federado.
- Experimentem com novas estratégias de tomada de decisão no simulador.

---

## Créditos e Referências

- **Autores do MobFogSim**: Este projeto se baseia no trabalho dos criadores do [MobFogSim](https://github.com/diogomg/MobFogSim). Todo o código do simulador na pasta `MobFogSim/` é derivado do repositório original.
- **Aprendizado Federado com Flower**: A configuração de aprendizado federado é viabilizada pelo [framework Flower](https://flower.dev).
- **Documentação e Implementação**:
  - Aprendizado federado: `ml_simple_model_training/PFG_modelo_federado.ipynb`.
  - API: `mobfogsim_migration_api/README.md`.
  - Simulador: `MobFogSim/README.md`.

Para dúvidas ou problemas, sinta-se à vontade para abrir uma issue neste repositório ou entrar em contato com os mantenedores do projeto.
