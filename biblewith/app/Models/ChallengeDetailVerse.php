<?php

namespace App\Models;

class ChallengeDetailVerse extends \CodeIgniter\Model
{
    protected $table = 'ChallengeDetailVerse';
    protected $allowedFields = [
        'chal_detail_verse_no',
        'chal_no',
        'bible_no',
        'is_checked',
        'progress_day'
    ];
    protected $primaryKey = 'chal_detail_verse_no';
}

