---
name: bug-report
description: Write a structured bug report for the FoodMe app. Use when the user
  describes a defect, unexpected behavior, or asks to report or log a bug.
---

# Bug report procedure

1. If any of these are missing from the user's description, ask for them
   before writing (ask all missing items in one message):
    - Exact steps to reproduce
    - Expected result vs actual result
    - Browser and device
2. Never invent steps, data, or results the user did not provide.
3. Assign severity using this scale and state the reason in one line:
    - Critical: blocks ordering or payment
    - Major: wrong data shown (prices, totals, restaurant info)
    - Minor: UI/cosmetic issue, workaround exists
4. Output in exactly this format:

**Title:** [Area] Short description of the defect
**Environment:** https://foodme-lusinevahanyan.onrender.com/ + browser/device
**Severity:** X — reason
**Steps to reproduce:**
1. ...
   **Expected result:**
   **Actual result:**
   **Notes:** (cold start possible? intermittent? attachments needed?)