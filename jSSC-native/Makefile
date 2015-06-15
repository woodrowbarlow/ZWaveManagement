VER = 2.8
ARCH=mips
CROSS_COMPILE=mips-linux-
CC=$(CROSS_COMPILE)gcc
CFLAGS=-Wall -fPIC -I. -shared

all:
	$(CC) $(CFLAGS) -o libjSSC-$(VER)_$(ARCH).so jssc.c

clean:
	rm -rf *.o *.so
