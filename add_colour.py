from sys import argv
from random import randint, choice

def add_colour(filename):
    hue = 0
    with open(filename, 'r') as f:
        for line in f:
            while 'black' in line:
                base_hue = choice([100,100,200])
                line = line.replace('black', 'hsla(%ideg,%i%%,%i%%,0.6)'%(randint(base_hue,base_hue + 50),
                                                                          randint(70,100),
                                                                          randint(20,40)), 1)
                #line = line.replace('black', 'hsl(%ideg,%i%%,%i%%)'%(hue, 100, 30), 1)
                hue = (hue + 38) % 360

                line = line.replace('stroke-width="0.2"', 'stroke-width="2.5"')
            print(line)

if __name__ == '__main__':
    add_colour(argv[1])
