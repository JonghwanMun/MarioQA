## Generation of QAs
After playing [Infinite Mario Bro.](../playingMario/README.md) and recording event logs, now we can generate QAs.


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
+ Annotation include following informations
  * `qa_id`: Question id
  * `video_path`: The form is `gameplay_id/gameplay_id_%d.dat`
  * `begin_frame`: The first frame number of video clip
  * `end_frame`: The last frame number of video clip
  * `clip_length`: The length of clip
  * `question`: The question of clip
  * `answer`: The answer of clip for the question
  * `event`: The target event in question, e.g. `Kill`, `Eat`, `Hit`, etc
  * `question_type`: The question type - `event-centric`, `counting`, `state`
  * `interrogative`: The interrogative of question, e.g. `How`, `When`, `Who`, etc
  * `temporal_relationship`: Difficulty of temporal relationship - `NT`, `ET`, `HT`
  * `semantic_chunk`: Semantic chunk of qeustion, e.g. `Event-Centric-Kill-How-Arg1-RedKoopa-Reference-After-Kill-Arg1-GreenKoopaWing`. This is added since our ICCV 2017 paper, so sematic chunk is not included in MarioQA data used for ICCV 2017.
  

### Download the event log files used in paper
```
$ bash run_download_event_logs.sh
```

### Download the filtered annotations used in paper 
```
$ bash run_download_filtered_annotation.sh
```
