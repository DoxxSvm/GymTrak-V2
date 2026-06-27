# Shop + Product + Advanced Inquiry Examples

## Create Advanced Inquiry

`POST /api/v1/inquiries`

```json
{
  "gymId": "gym_123",
  "first_name": "John",
  "last_name": "Doe",
  "phone": "9876543210",
  "date": "2026-03-24",
  "gender": "male",
  "address": "Mumbai",
  "source": "walk-in",
  "interest": "weight loss",
  "notes": "Interested in annual plan"
}
```

## Add Product

`POST /api/v1/products`

```json
{
  "gymId": "gym_123",
  "name": "Whey Protein",
  "category": "supplements",
  "stock_quantity": 50,
  "cost_price": 1000,
  "selling_price": 1500,
  "description": "Premium whey",
  "image_url": "https://cdn.example.com/whey.jpg"
}
```

## Update Stock

`PUT /api/v1/products/{id}/stock?gymId=gym_123`

```json
{
  "quantity": 10,
  "type": "subtract"
}
```
