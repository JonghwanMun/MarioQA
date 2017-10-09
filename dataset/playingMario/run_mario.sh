#! /bin/bash

if [ $# -eq 1 ]; then
	fps=45
else
	fps=$2
fi

java -cp target/Mario-1.0-jar-with-dependencies.jar \
	com.mojang.mario.FrameLauncher \
	-gid $1 -fps ${fps}
