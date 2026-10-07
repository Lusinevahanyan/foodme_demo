# General QA rules

## Environment
- App under test: FoodMe, deployed at https://foodme-lusinevahanyan.onrender.com/
- Base URL must come from config (BASE_URL), never hardcoded in tests.
- The app is hosted on Render and may cold-start (~30-60s) after inactivity.
  If the first request in a run times out, retry once before reporting a failure,
  and say in the report that it may be a cold start.

## Test integrity
- Never delete, skip, or weaken a failing test to make CI pass.
  Report the failure and the likely cause instead.
- Never change an expected value just to match the actual output.
  If expected and actual differ, explain which one is wrong and why.