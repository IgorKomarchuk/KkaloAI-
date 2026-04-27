import os
import pandas as pd
from google.analytics.admin_v1alpha import AnalyticsAdminServiceClient, CustomDimension
from google.analytics.data_v1beta import BetaAnalyticsDataClient, RunReportRequest, DateRange, Dimension, Metric
from google.oauth2 import service_account

# Configuration
KEY_PATH = r"D:\Cal AI\snapcal-9b8e5-12fc081e96c8.json"
PROPERTY_ID = "533655786" # snapcal-9b8e5

class AnalyticsExpert:
    def __init__(self, key_path, property_id):
        self.key_path = key_path
        self.property_id = f"properties/{property_id}"
        os.environ["GOOGLE_APPLICATION_CREDENTIALS"] = key_path
        self.admin_client = AnalyticsAdminServiceClient()
        self.data_client = BetaAnalyticsDataClient()

    def setup_custom_dimensions(self):
        """Ensures that nutritional dimensions are configured in GA4."""
        print("--- Checking Custom Dimensions configuration ---")
        
        # Dimensions to ensure
        dimensions = [
            ("food_name", "Name of the recognized food"),
            ("calories", "Total calories (numeric)"),
            ("proteins", "Proteins in grams"),
            ("carbs", "Carbohydrates in grams"),
            ("fats", "Fats in grams")
        ]

        existing = [cd.parameter_name for cd in self.admin_client.list_custom_dimensions(parent=self.property_id)]
        
        for name, description in dimensions:
            if name not in existing:
                print(f"   [+] Creating Custom Dimension: {name}")
                cd = CustomDimension()
                cd.parameter_name = name
                cd.display_name = name.replace("_", " ").title()
                cd.scope = CustomDimension.DimensionScope.EVENT
                cd.description = description
                self.admin_client.create_custom_dimension(parent=self.property_id, custom_dimension=cd)
            else:
                print(f"   [ok] {name} already exists.")

    def run_expert_report(self):
        """Fetches data and provides business insights."""
        print("\n--- Generating Expert Analysis Report ---")
        
        request = RunReportRequest(
            property=self.property_id,
            dimensions=[Dimension(name="eventName"), Dimension(name="customEvent:food_name")],
            metrics=[Metric(name="eventCount")],
            date_ranges=[DateRange(start_date="7daysAgo", end_date="today")]
        )

        try:
            response = self.data_client.run_report(request)
            
            data = []
            for row in response.rows:
                data.append({
                    "Event": row.dimension_values[0].value,
                    "Food": row.dimension_values[1].value,
                    "Count": int(row.metric_values[0].value)
                })

            df = pd.DataFrame(data)
            if df.empty:
                print("📝 Insight: No data collected in the last 7 days yet. Start logging meals to see analysis.")
                return

            # Expert Synthesis
            print("\n--- PROFESSIONAL INSIGHTS ---")
            total_scans = df[df['Event'] == 'food_recognized']['Count'].sum()
            total_saves = df[df['Event'] == 'meal_saved']['Count'].sum()
            
            conv_rate = (total_saves / total_scans * 100) if total_scans > 0 else 0
            
            print(f"📈 Total Foods Recognized: {total_scans}")
            print(f"💾 User Retention (Save Rate): {conv_rate:.1f}%")
            
            top_foods = df[df['Event'] == 'food_recognized'].sort_values(by='Count', ascending=False).head(3)
            print(f"🍕 Top Foods Scanned: {', '.join(top_foods['Food'].tolist())}")

            if conv_rate < 50:
                print("⚠️ ADVICE: High abandonment rate found. Users are scanning food but not saving it. Check if Gemini's results look accurate or if the UI is confusing.")
            else:
                print("✅ ADVICE: Excellent engagement. Users trust the AI results and are building their history.")

        except Exception as e:
            print(f"ERROR: Could not run report: {str(e)}")

if __name__ == "__main__":
    expert = AnalyticsExpert(KEY_PATH, PROPERTY_ID)
    expert.setup_custom_dimensions()
    expert.run_expert_report()
