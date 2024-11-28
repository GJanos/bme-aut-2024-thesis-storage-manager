package com.bme.vik.aut.thesis.depot.general.admin.category;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class CategoryRepositoryTest {

    private final static String CATEGORY_NAME = "category";
    private final static String CATEGORY_DESCRIPTION = "description";

    @Autowired
    private CategoryRepository categoryRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        //***** <-- given --> *****//
        Category category = Category.builder()
                .name(CATEGORY_NAME)
                .description(CATEGORY_DESCRIPTION)
                .build();

        categoryRepository.save(category);
    }

    @AfterEach
    void tearDown() {
        categoryRepository.deleteAll();
    }

    @Test
    void shouldReturnCategoryWhenCategoryExists() {
        //***** <-- when --> *****//
        boolean exists = categoryRepository.existsByName(CATEGORY_NAME);

        //***** <-- then --> *****//
        assertThat(exists).isTrue();
    }

    @Test
    void shouldNotReturnCategoryWhenCategoryDoesNotExist() {
        //***** <-- when --> *****//
        String categoryName = "nonexistentcategory";
        boolean exists = categoryRepository.existsByName(categoryName);

        //***** <-- then --> *****//
        assertThat(exists).isFalse();
    }
}