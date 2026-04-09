What bugs you found and how you fixed them
------------------------------------------

- Missing Failed Status for Payments
- Incorrect usage when rounding BigDecimals
- No updates being made in catches 

Your approach to the refund and status endpoints
------------------------------------------------

My approach for the refund was to create a new object specifically for refunds in the case of wanting more data
on the amount of refunds that had occured and the details about the amount and the customer it belonged to, this would be better for reporting,
and the creation of services to specifically handle refunds. However it is largely similar to the payment object.
I had also moved it to its own service in the interest of separation of concerns.

For the StatusHistory, I had created a new object that was tied to individual payments and contained a list of all 
the updates to the status made for the payment, I did this as it was cleaner and seperated concerns between the payment
and its history, similar to the refund above.

Test strategy and what you covered
----------------------------------

I had just made some simple unit tests with mocking, I covered a general positive path for the refund, 
an instance where the payment could not be found. 
And lastly a test for an exception being thrown in the service and being returned as a 500

Any improvements you made and why
---------------------------------

I had added an enum for the different RoundingScales to avoid magic/hardcoded values, 
and allow easier inference of what the rounding scale should be.

Anything you'd do differently with more time
--------------------------------------------
- More detailed testing
- Less usage of !! and ?, rather do proper null checks etc
- Cleaner services, less duplication of code, more helper methods/classes to reduce noise and improve readability
- Slim down the statusHistory
- Improve naming conventions
- Create more concrete relationships between the entities
- Better validation and exception handling
- Move methods for StatusHistory into its own service to keep separation of concerns
- Added verification that the refund being requested was by the same user that had made the payment