INSERT INTO public.category (id, category) VALUES (1, 'HAMBURGUER');
INSERT INTO public.category (id, category) VALUES (2, 'DRINK');
INSERT INTO public.category (id, category) VALUES (3, 'DESSERT');


INSERT INTO public.product (id, code, description, category_id) VALUES (1, 'X-BACON', 'Hamburger bun, hamburger, cheese, egg, lettuce and mayonnaise', 1);
INSERT INTO public.product (id, code, description, category_id) VALUES (2, 'X-SALAD', 'Hamburger bun, hamburger and cheese ', 1);
INSERT INTO public.product (id, code, description, category_id) VALUES (3, 'CHEESEBURGER', 'Hamburger bun, hamburger, cheese, corn, lettuce, mayonnaise and tomato', 1);
INSERT INTO public.product (id, code, description, category_id) VALUES (4, 'X-EGG', 'Hamburger bun, hamburger, cheese, egg, lettuce, mayonnaise and tomato', 1);
INSERT INTO public.product (id, code, description, category_id) VALUES (5, 'X-CHICKEN', 'Hamburger bun, chicken fillet, lettuce, tomato, corn, patata straw, mayonnaise, ham and cheese', 1);
INSERT INTO public.product (id, code, description, category_id) VALUES (6, 'X-EVERYTHING', 'Hamburger bun, hamburger, sausage, straw potatoes, cheese, egg, lettuce, mayonnaise and tomato', 1);
INSERT INTO public.product (id, code, description, category_id) VALUES (7, 'X-EVERYTHING CHICKEN', 'Hamburger bun, hamburger, chicken, sausage, straw potatoes, cheese, egg, lettuce, mayonnaise and tomato', 1);


INSERT INTO public.product (id, code, description, category_id) VALUES (8, 'COKE 600ml', 'Coca-cola can 600ml', 2);
INSERT INTO public.product (id, code, description, category_id) VALUES (9, 'COKE', 'Coca-cola can 2L', 2);
INSERT INTO public.product (id, code, description, category_id) VALUES (10, 'FANTA ORANGE', 'Fanta Orange can 2L', 2);
INSERT INTO public.product (id, code, description, category_id) VALUES (11, 'GUARANA', 'Guarana can 2L', 2);
INSERT INTO public.product (id, code, description, category_id) VALUES (12, 'GUARANA 600ml', 'Guarana can 600ml', 2);

INSERT INTO public.product (id, code, description, category_id) VALUES (13, 'ICE-CREAM', 'Vanilla ice cream', 3);
INSERT INTO public.product (id, code, description, category_id) VALUES (14, 'PASSION FRUIT MOUSSE', 'Passion fruit mousse', 3);
INSERT INTO public.product (id, code, description, category_id) VALUES (15, 'STRAWBERRY PIE', 'strawberry pie', 3);