<?php

namespace App\Models;

class Note extends \CodeIgniter\Model
{
    protected $table = 'Note';
    protected $allowedFields = ['note_no', 'user_no', 'note_content', 'note_date' ];
    protected $primaryKey = 'note_no';
}

