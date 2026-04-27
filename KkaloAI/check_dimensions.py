import os
from google.analytics.admin_v1alpha import AnalyticsAdminServiceClient

# Configuration
KEY_PATH = r"D:\Cal AI\snapcal-9b8e5-12fc081e96c8.json"
PROPERTY_ID = "properties/533655786"

def check_dimensions():
    os.environ["GOOGLE_APPLICATION_CREDENTIALS"] = KEY_PATH
    client = AnalyticsAdminServiceClient()
    print("Checking Custom Dimensions...")
    dimensions = client.list_custom_dimensions(parent=PROPERTY_ID)
    found = False
    for cd in dimensions:
        found = True
        print(f" - Found dimension: {cd.parameter_name} ({cd.display_name})")
    if not found:
        print("No custom dimensions found.")

if __name__ == "__main__":
    check_dimensions()
