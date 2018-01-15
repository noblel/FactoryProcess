package com.noblel.factory;


import com.noblel.factoryprocessor.annotation.Factory;

/**
 * @author Noblel
 */
@Factory(id = "Margherita", type = Meal.class)
public class MargheritaPizza implements Meal {

    @Override
    public float getPrice() {
        return 6f;
    }
}
