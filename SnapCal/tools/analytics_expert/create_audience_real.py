import os
from google.analytics.admin_v1alpha import AnalyticsAdminServiceClient, Audience, AudienceEventFilter, AudienceFilterClause, AudienceFilterExpression, AudienceFilterExpressionList, AudienceSimpleFilter

# Configuration
KEY_PATH = r"D:\Cal AI\snapcal-9b8e5-12fc081e96c8.json"
PROPERTY_ID = "properties/533655786"

def create_real_audience():
    os.environ["GOOGLE_APPLICATION_CREDENTIALS"] = KEY_PATH
    client = AnalyticsAdminServiceClient()
    
    print("Expert Agent: Creating real custom audience 'Active Calorie Trackers'...")
    
    audience = Audience()
    audience.display_name = "Active Calorie Trackers (Expert)"
    audience.description = "Users who have at least one 'meal_saved' event. Created by AI Expert."
    audience.membership_duration_days = 30
    
    # Filter: includes users who have the 'meal_saved' event
    event_filter = AudienceEventFilter()
    event_filter.event_name = "meal_saved"
    
    expression = AudienceFilterExpression()
    expression.event_filter = event_filter
    
    clause = AudienceFilterClause()
    clause.simple_filter = AudienceSimpleFilter()
    clause.simple_filter.inclusion_duration_seconds = 0 # membership duration
    clause.simple_filter.filter_expression = expression
    
    audience.filter_clauses.append(clause)
    
    try:
        response = client.create_audience(parent=PROPERTY_ID, audience=audience)
        print(f"SUCCESS: Expert Audience created! ID: {response.name}")
        print("Refresh GA4 'Audiences' now. You will see 'Active Calorie Trackers (Expert)'.")
    except Exception as e:
        print(f"ERROR: Could not create audience: {str(e)}")

if __name__ == "__main__":
    create_real_audience()
