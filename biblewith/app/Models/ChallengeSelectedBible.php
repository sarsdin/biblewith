<?php

namespace App\Models;

class ChallengeSelectedBible extends \CodeIgniter\Model
{
    protected $table = 'ChallengeSelectedBible';
    protected $allowedFields = [
        'chal_selected_bible_no',
        'chal_no',
        'book'
    ];
    protected $primaryKey = 'chal_selected_bible_no';
}