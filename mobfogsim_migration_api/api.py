import logging
from fastapi import FastAPI
from pydantic import BaseModel
import torch
import torch.nn.functional as F
import numpy as np

logging.basicConfig(filename='logs.txt', level=logging.INFO, format='%(asctime)s %(levelname)s: %(message)s')

app = FastAPI()

MODEL_PATH = "decision_model.pth"
DEVICE = torch.device("cuda" if torch.cuda.is_available() else "cpu")

class MigrationModel(torch.nn.Module):
    """
    A PyTorch model for migration decision.
    """
    def __init__(self, input_dim=2):
        super(MigrationModel, self).__init__()
        self.fc1 = torch.nn.Linear(input_dim, 4)
        self.fc2 = torch.nn.Linear(4, 1)

    def forward(self, x):
        """
        Forward pass for the model.
        """
        x = F.relu(self.fc1(x))
        x = self.fc2(x)
        return x

def load_model():
    """
    Loads the trained model from the specified checkpoint.
    """
    checkpoint = torch.load(MODEL_PATH, map_location=DEVICE)
    input_dim = checkpoint['input_dim']
    model = MigrationModel(input_dim=input_dim).to(DEVICE)
    model.load_state_dict(checkpoint['model_state_dict'])
    model.eval()
    return model

try:
    model = load_model()
    logging.info("Model loaded successfully")
except Exception as e:
    logging.error(f"Error loading model: {e}")

class RequestData(BaseModel):
    """
    Request body structure.
    """
    PosX: float
    PosY: float
    Direction: float
    Speed: float
    DistanceToSourceAp: float
    DistanceToLocalCloudlet: float
    DistanceToClosestCloudlet: float
    IsMigPoint: bool
    IsMigZone: bool

class ResponseData(BaseModel):
    """
    Response body structure.
    """
    shouldMigrate: bool

def run_inference(is_mig_point: bool, is_mig_zone: bool) -> bool:
    """
    Runs inference on the model given the migration point and zone status.
    Returns True if migration should occur, otherwise False.
    """
    mig_point_val = 1 if is_mig_point else 0
    mig_zone_val = 1 if is_mig_zone else 0
    input_features = np.array([mig_point_val, mig_zone_val], dtype=np.float32).reshape(1, -1)
    input_tensor = torch.tensor(input_features).to(DEVICE)
    with torch.no_grad():
        logits = model(input_tensor).squeeze()
        prob = torch.sigmoid(logits).item()
    return prob > 0.5

@app.post("/should_migrate", response_model=ResponseData)
async def should_migrate(data: RequestData):
    """
    Endpoint for determining if a device should migrate.
    """
    logging.info(f"Received request: {data.json()}")
    decision = run_inference(data.IsMigPoint, data.IsMigZone)
    logging.info(f"Decision: {decision} for request: PosX={data.PosX}, PosY={data.PosY}, Direction={data.Direction}, Speed={data.Speed}")
    return ResponseData(shouldMigrate=decision)
