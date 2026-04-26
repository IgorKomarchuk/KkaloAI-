import os
from google.analytics.admin_v1alpha import AnalyticsAdminServiceClient

def verify_access(key_path):
    # Set the environment variable for authentication
    os.environ["GOOGLE_APPLICATION_CREDENTIALS"] = key_path
    
    try:
        client = AnalyticsAdminServiceClient()
        # Try to list account summaries which this service account has access to
        print("Checking Google Analytics access for service account...")
        accounts = client.list_account_summaries()
        
        found_any = False
        for account in accounts:
            found_any = True
            print(f"SUCCESS: Access confirmed to Account: {account.display_name} ({account.account})")
            # List properties for this account
            for prop in account.property_summaries:
                print(f"   -> Property: {prop.display_name} ({prop.property})")
        
        if not found_any:
            print("WARNING: Connected, but NO accounts found. Please make sure you added the email as an Administrator to the GA4 PROPERTY.")
            return False
        return True
    except Exception as e:
        print(f"ERROR: Could not connect to Google Analytics. Details: {str(e)}")
        return False

if __name__ == "__main__":
    KEY_PATH = r"D:\Cal AI\snapcal-9b8e5-12fc081e96c8.json"
    verify_access(KEY_PATH)
