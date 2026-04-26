import os
from google.analytics.admin_v1alpha import AnalyticsAdminServiceClient, Audience, AudienceSimpleFilter, AudienceFilterClause, AudienceFilterExpression, AudienceEventFilter

# Configuration
KEY_PATH = r"D:\Cal AI\snapcal-9b8e5-12fc081e96c8.json"
PROPERTY_ID = "properties/533655786"

def create_rock_solid_audience():
    os.environ["GOOGLE_APPLICATION_CREDENTIALS"] = KEY_PATH
    client = AnalyticsAdminServiceClient()
    
    print("Expert Agent: Final fix for audience creation...")
    
    audience = Audience()
    audience.display_name = "Active Calorie Trackers (Expert)"
    audience.description = "Identified by AI Agent as users who save meals."
    audience.membership_duration_days = 30
    
    # Filter Expression
    filter_expression = AudienceFilterExpression(
        event_filter=AudienceEventFilter(event_name="meal_saved")
    )
    
    # Filter Clause with explicit Type and Scope
    clause = AudienceFilterClause()
    clause.clause_type = AudienceFilterClause.AudienceClauseType.INCLUDE
    clause.simple_filter = AudienceSimpleFilter(
        scope=AudienceSimpleFilter.AudienceFilterScope.AUDIENCE_FILTER_SCOPE_ACROSS_ALL_SESSIONS,
        filter_expression=filter_expression
    )
    
    audience.filter_clauses.append(clause)
    
    try:
        response = client.create_audience(parent=PROPERTY_ID, audience=audience)
        print(f"SUCCESS: Expert Audience created! ID: {response.name}")
        print("Refresh GA4 'Audiences' now.")
    except Exception as e:
        print(f"ERROR: {str(e)}")

if __name__ == "__main__":
    create_rock_solid_audience()
