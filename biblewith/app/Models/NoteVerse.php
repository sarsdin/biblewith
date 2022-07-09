<?php

namespace App\Models;

class NoteVerse extends \CodeIgniter\Model
{
    protected $table = 'NoteVerse';
    protected $allowedFields = ['note_verse_no', 'note_no', 'bible_no' ];
    protected $primaryKey = 'note_verse_no';
}
