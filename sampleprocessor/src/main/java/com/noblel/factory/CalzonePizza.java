package com.noblel.factory;


import com.noblel.factoryprocessor.annotation.Factory;

/**
 * @author Noblel
 */
@Factory(id = "Calzone", type = Meal.class)
public class CalzonePizza implements Meal {

    @Override
    public float getPrice() {
        return 8.5f;
    }
}
