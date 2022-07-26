<?php

namespace App\Models;

class ChallengeDetail extends \CodeIgniter\Model
{
    protected $table = 'ChallengeDetail';
    protected $allowedFields = [
        'chal_no',
        'bible_no',
        'progress_day'
    ];
    protected $primaryKey = 'chal_no';
}

