## Playing Infinite Mario Bro.

The Infinite Mario Bro. is originally written by Minecraft creator MarKus "Notch" Persson, which is able to randomly generate levels. [The original version](https://www.mojang.com/notch/mario/) does not exist anymore, so we use [the archived one](https://github.com/cflewis/Infinite-Mario-Bros). We additionally add some functions to record the log of events occuring in gameplay for MarioQA dataset.

### Setup instructions
+ Install JDK, JRE in [oracle](http://www.oracle.com/technetwork/pt/java/javase/downloads/index.html).

### How to run?
1. Type and run following line to play Infinite Mario Bro.
  ```
  $ bash run_mario.sh your_gameplay_id your_fps
  ``` 
&nbsp;&nbsp;
  There are two parameters: `your_gameplay_id` and `your_fps`.
  * `your_gameplay_id`: id (or name) of gameplay. The outputs are distinguished with this value.
  * `your_fps`: controls the speed of gameplay. The default value of `your_fps` is 45 resulting in 22 fps in our environment.
    If the speed of gamplay is too slow or fast, then adjust the fps. <br />

&nbsp;&nbsp;
  Key mappings:
  * `a`: Running or holding a shell.
  * `s`: Jumping.
  * `d`: Shooting a fireball.

2. Press **F12** to finish the gamplay.

&nbsp;&nbsp;
Then, two outputs are saved as follows: 
  * Frames: `gameplay/gameplay_id/gameplay_id_#frame.dat` 
  * Event log: `event_logs/gameplay_id.json` <br>
  
  &nbsp;&nbsp;
  For example, if gameplay_id is `play1`, the outputs are `gameplay/play1/play1_#frame.dat` and `event_logs/play1.json`)<br>


### Event log
Event log contains 11 events (`Kill, Die, Junp, Hit, Break, Appear, Shoot, Throw, Kick, Hold, Eat`) and is saved as json file.
Each event include following information:
  * `frame`: the frame where event occurs
  * `sceneState`: the state of scene (cave|castle|field)
  * `marioState`: the state of mario (tinyform|superform|fireform)
  * `spriteLocation`: location of event's target when the event occurs
  * `marioLocation`: location of mario when the event occurs
  * `action`: the type of event
  * `target`: the target of event
  * `means`: the mean of event

### Download the clips of MarioQA dataset
Please follow the instruction in [project page](http://cvlab.postech.ac.kr/research/marioQA)

### LICENSE      

The software is composed with three resources: art resources, code for playing game and code for recording event logs. As Notch mentioned in [LICENSE](https://github.com/BillyWM/Infinite-Mario/blob/master/doc/LICENSE.txt) and [README](https://github.com/BillyWM/Infinite-Mario/blob/master/doc/README.txt), the art resources of Infinite Mario Bro. (images and sounds) are owned by Nintendo and the source code by Notch is public domain. The codes for recording event logs, which are written by us, is being made available for individual research purpose only. Thus, we release the software with research purpose only as in [Mario AI Championship](http://www.marioai.org/) (2009-2012) which uses Infinite Mario Bro. for research purpose.
