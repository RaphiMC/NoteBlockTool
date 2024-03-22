# NoteBlockTool
Tool for importing, exporting, batch manipulating and playing Minecraft note block songs.

To download the latest stable version, go to [GitHub Releases](https://github.com/RaphiMC/NoteBlockTool/releases/latest).  
To download the latest dev version, go to [GitHub Actions](https://github.com/RaphiMC/NoteBlockTool/actions/workflows/build.yml) or [Lenni0451's Jenkins](https://build.lenni0451.net/job/NoteBlockTool/).

Using it is very simple, just run the jar file, and it will start a user interface. Detailed instructions can be found in the [Usage](#usage) section.

## Features
- Reads .nbs, .mcsp2, .mid, .txt and .notebot files
- Can export all of the above as .nbs
- Work with multiple songs at once (Batch processing)
- High performance and accurate song player
  - Lag free playback of very large songs
  - Supports all NBS features except for custom instruments
- Good MIDI importer
  - Supports most MIDI files
  - Supports velocity and panning
  - Can handle Black MIDI files
- Supports all NBS versions
  - Version 0 - 5
  - Supports undocumented features like Tempo Changers
- Many tools for manipulating songs
    - Optimize songs for use in Minecraft (Transposing, Resampling)
    - Resampling songs with a different TPS
    - NBS metadata editor
    - Instrument replacement
    - Note deduplication (Useful for Black MIDI files)
    - Removal of quiet notes (Useful for Black MIDI files)
- Very fast and efficient
  - Imports and manipulates hundreds of songs in seconds

### Limitations
- NBS layers are not preserved when doing anything other than editing the metadata
  - All editing is done on intermediary song objects which do not have layers
- NBS custom instruments aren't played back

## Usage
### Importing
After downloading and running the jar file, you will be greeted with a user interface:  
![NoteBlockTool Main GUI](img/NoteBlockTool%20Main%20GUI.png)  
The table shows all currently loaded songs. Songs can be dragged and dropped into the table to load them or you can use the "Add" button to open a file dialog.
### Editing
To start editing songs, select one or multiple songs in the table and click the "Edit" button. This will open the song editor:  
![NoteBlockTool Song Editor](img/NoteBlockTool%20Song%20Editor.png)  
By default, everything is disabled or does nothing. To enable a feature, click the corresponding checkbox or configure its settings.  
After you are done editing, click the "Save" button to apply the edits to the selected songs or the "Preview" button to listen to the edits before saving.  
### Playing
To play a song, select it in the table and click the "Play" button. This will open the song player:  
![NoteBlockTool Song Player](img/NoteBlockTool%20Song%20Player.png)  
The song player features all the essential controls for playing a song.
### Exporting
To export the edited songs, select them in the table and click the "Export" button. This will open the export dialog where you can select a folder to save all the songs to.

## Using it in your application
NoteBlockTool uses [NoteBlockLib](https://github.com/RaphiMC/NoteBlockLib) for most of its functionality. For more information on how to use NoteBlockLib in your application, check out [NoteBlockLib](https://github.com/RaphiMC/NoteBlockLib).

## Contact
If you encounter any issues, please report them on the
[issue tracker](https://github.com/RaphiMC/NoteBlockTool/issues).  
If you just want to talk or need help using NoteBlockTool feel free to join my
[Discord](https://discord.gg/dCzT9XHEWu).
