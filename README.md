<h1 align="center">
  Calculatus: Math In Minecraft Chat
</h1>

## What it does
Calculatus is a Forge mod for Minecraft 1.8.9 that enabled you to evaluate mathematical expressions in chat. It supports basic arithmetic operations, parentheses, and modulo. Your player position is stored in the variables x,y,z which correspond to the player's cordinates.
* `/calc <expression>`: Evaluate a mathematical expression.
  * Example usage: `/calc 2 + 2 * 3` evaluates to `8`.
  * Example usage: `/calc 1/2` evaluates to `0.5`. (note: rounds to the nearest hundredths digit)
  * Example usage: `/calc z+10` (where the player's position is at z = -10) evaluates to `0`.
* `/calc history`: View the history of expressions you have evaluated.
* `/calc clear`: Clear the history of expressions you have evaluated.

## How to install
* Click on latest releases and drag the jar file into your mods folder

### Alternative Installation
* Install from [Modrinth](https://modrinth.com/mod/calculatus)