FROM python:3.10-slim

# Definir o diretório de trabalho
WORKDIR /app

# Copiar requirements.txt para a imagem
COPY requirements.txt /app/requirements.txt

# Instalar dependências
RUN pip install --no-cache-dir -r requirements.txt
RUN pip install torch==2.5.1 --extra-index-url https://download.pytorch.org/whl/cpu

# Copiar o restante dos arquivos para a imagem
COPY . /app

# Expor a porta do servidor
EXPOSE 8000

# Comando de inicialização do servidor uvicorn
CMD ["uvicorn", "api:app", "--host", "0.0.0.0", "--port", "8000"]
