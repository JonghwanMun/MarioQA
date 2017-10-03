## Generation of QAs
After playing [Infinite Mario Bro.](JonghwanMun/MarioQA/tree/master/dataset/playingMario) and recording event logs, now we can generate QAs.


### Setup instructions
+ Install JDK, JRE and maven.
+ Compile the source code with following line.

  ```
  $ bash run_compile.sh
  ```


### Run to generate QAs
```
$ bash run_generation_QA.sh
```
+ Inputs
  * `data/logFileLists.txt`: The filename list of event logs to generate QAs.
  * `data/configuration.json`: Options for sampling clip region (fps, margin, durations).
  * `data/template.json`: Pre-defined question template.
  * `data/phrase.json`: Pre-defined phrase (or word) mapping to fill the question template or to obtain answer phrase.
+ Outputs
  * `generated_annotations/$gameplay_id$_raw_annotations.json`: Raw annotation for each gameplay. 
  * `generated_annotations/filtered_annotations_NT.json`: Filtered out annotations with only NT questions.
  * `generated_annotations/filtered_annotations_ET.json`: Filtered out annotations with only ET questions.
  * `generated_annotations/filtered_annotations_HT.json`: Filtered out annotations with only HT questions.
  * `generated_annotations/filtered_annotations_ALL.json`: Filtered out annotations with all questions.
  
  
### Download the event log files (used in construction of MarioQA dataset)
```
$ bash run_download_event_logs.sh
```
