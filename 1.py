import pygame
import sys
import random

# Pygame inicialization
pygame.init()

# Screen size
screen_width = 800
screen_height = 600
screen = pygame.display.set_mode((screen_width, screen_height))

# Colors
black = (0, 0, 0)
white = (255, 255, 255)

# Player properties
player_size = 50
player_color = white

# Snake properties
snake_speed = 10
snake_length = 5

# Food properties
food_size = 20
food_color = black

# Score
score = 0

class Player(pygame.Rect):
    def __init__(self):
        super().__init__(screen_width / 2, screen_height / 2, player_size, player_size)

    def move(self, dx, dy):
        self.x += dx
        self.y += dy

        if self.x < 0:
            self.x = screen_width
        elif self.x > screen_width:
            self.x = 0
        if self.y < 0:
            self.y = screen_height
        elif self.y > screen_height:
            self.y = 0

class Snake(pygame.Rect):
    def __init__(self, x, y):
        super().__init__(x, y, snake_length * player_size, player_size)

    def move(self, dx, dy):
        head = pygame.Rect(self.x + dx, self.y + dy, player_size, player_size)
        if not head.colliderect(player):
            self.x += dx
            self.y += dy

class Food(pygame.Rect):
    def __init__(self):
        super().__init__(random.randint(0, screen_width - food_size) // food_size * food_size,
                         random.randint(0, screen_height - food_size) // food_size * food_size, food_size,
food_size)

def main():
    global score
    clock = pygame.time.Clock()
    player = Player()

    snake = Snake(screen_width / 2, screen_height / 2)
    food = Food()

    while True:
        for event in pygame.event.get():
            if event.type == pygame.QUIT:
                pygame.quit()
                sys.exit()
            elif event.type == pygame.KEYDOWN:
                if event.key == pygame.K_LEFT:
                    snake.move(-snake_speed, 0)
                elif event.key == pygame.K_RIGHT:
                    snake.move(snake_speed, 0)
                elif event.key == pygame.K_UP:
                    snake.move(0, -snake_speed)
                elif event.key == pygame.K_DOWN:
                    snake.move(0, snake_speed)

        screen.fill(black)
        pygame.draw.rect(screen, player_color, player)
        pygame.draw.rect(screen, food_color, food)

        if snake.x < food.x and snake.y < food.y:
            score += 1
            food = Food()
        elif snake.x > food.x and snake.y > food.y:
            score -= 1
            food = Food()

        pygame.display.update()
        clock.tick(snake_speed * 2)

if __name__ == "__main__":
    main()