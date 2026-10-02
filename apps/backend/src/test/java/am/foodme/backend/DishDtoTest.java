package am.foodme.backend;

import am.foodme.backend.dto.DishDto;
import am.foodme.backend.model.Dish;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DishDtoTest {

    @Test
    void mapEntityToDto_nullArmenianName_doesNotThrow() {
        Dish dish = new Dish();
        dish.setNameEn("Pizza");
        dish.setNameAm(null);

        DishDto dto = DishDto.mapEntityToDto(dish);

        assertEquals("Pizza", dto.getNameEn());
        assertNull(dto.getNameHy());
    }

    @Test
    void mapEntityToDto_trimsArmenianName() {
        Dish dish = new Dish();
        dish.setNameAm("  Պիցցա  ");

        assertEquals("Պիցցա", DishDto.mapEntityToDto(dish).getNameHy());
    }
}