package io.github.valossa515.pizzatracker.query;

import io.github.valossa515.pizzatracker.domain.Order;
import io.github.valossa515.spring_courier.core.interfaces.IQuery;
import io.github.valossa515.spring_courier.core.support.Response;

import java.util.List;

public class GetActiveOrdersQuery implements IQuery<Response<List<Order>>> {
}
