import os
from google.analytics.admin_v1alpha import AnalyticsAdminServiceClient, Audience, AudienceSimpleFilter, AudienceFilterClause, AudienceFilterExpression, AudienceEventFilter

# Configuration
KEY_PATH = r"D:\Cal AI\snapcal-9b8e5-12fc081e96c8.json"
PROPERTY_ID = "properties/533655786"

def create_final_audience():
    os.environ["GOOGLE_APPLICATION_CREDENTIALS"] = KEY_PATH
    client = AnalyticsAdminServiceClient()
    
    print("Expert Agent: Final attempt to create proof audience...")
    
    audience = Audience()
    audience.display_name = "Active Calorie Trackers (Expert)"
    audience.description = "Users who log meals. Created via AI API."
    audience.membership_duration_days = 30
    
    # Filter Expression
    filter_expression = AudienceFilterExpression(
        event_filter=AudienceEventFilter(event_name="meal_saved")
    )
    
    # Filter Clause with explicit Type
    clause = AudienceFilterClause()
    clause.clause_type = AudienceFilterClause.AudienceClauseType.INCLUDE
    clause.simple_filter = AudienceSimpleFilter(
        filter_expression=filter_expression
    )
    
    audience.filter_clauses.append(clause)
    
    try:
        response = client.create_audience(parent=PROPERTY_ID, audience=audience)
        print(f"SUCCESS: Expert Audience created! ID: {response.name}")
    except Exception as e:
        print(f"ERROR: Could not create audience: {str(e)}")

if __name__ == "__main__":
    create_final_audience()
